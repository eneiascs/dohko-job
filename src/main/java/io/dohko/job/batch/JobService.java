/**
 *     Copyright (C) 2013-2017  the original author or authors.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License,
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package io.dohko.job.batch;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.excalibur.core.execution.domain.Application;
import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.Block;
import org.excalibur.core.execution.domain.JobStatus;
import org.excalibur.core.execution.domain.Precondition;
import org.excalibur.core.execution.domain.TaskOutput;
import org.excalibur.core.execution.domain.TaskOutputType;
import org.excalibur.core.execution.domain.TaskStats;
import org.excalibur.core.execution.domain.TaskStatus;
import org.excalibur.core.execution.domain.TaskStatusType;
import org.excalibur.core.execution.domain.repository.BlockRepository;
import org.excalibur.core.execution.domain.repository.JobRepository;
import org.excalibur.core.execution.domain.repository.TaskCpuStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskMemoryStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskOutputRepository;
import org.excalibur.core.execution.domain.repository.TaskRepository;
import org.excalibur.core.execution.domain.repository.TaskStatusRepository;
import org.excalibur.core.host.repository.PackageRepository;
import org.excalibur.core.json.databind.ObjectMapperUtil;
import org.excalibur.core.util.concurrent.DynamicExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

import io.airlift.command.ProcessCpuState;
import io.airlift.command.ProcessMemoryState;
import io.airlift.command.ProcessState;
import io.dohko.job.batch.tree.Tree;
import io.dohko.job.batch.tree.TreeNode;
import io.dohko.job.batch.tree.TreeTraversalOrderType;
import io.dohko.job.host.Package;
import io.dohko.job.host.PackageManagerType;
import job.flow.Flow;
import job.flow.Job;
import job.flow.Step;

import static java.lang.Math.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.hash.Hashing.sha256;
import static io.airlift.command.Command.newBashCommand;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.excalibur.core.util.SystemUtils2.getLongProperty;
import static io.airlift.command.CommandBuilder.*;
import static org.apache.commons.io.FilenameUtils.*;

import static org.excalibur.core.util.Instants.*;
import static org.excalibur.core.execution.domain.TaskStatus.*;
import static org.excalibur.core.execution.domain.TaskStatusType.CANCELLED;

@Service
public class JobService {
	private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
	private final JobRepository jobRepository;

	private final TaskRepository taskRepository;
	private final TaskStatusRepository taskStatusRepository;
	private final TaskCpuStatsRepository taskCpuStatsRepository;
	private final TaskMemoryStatsRepository taskMemoryStatsRepository;
	private final TaskOutputRepository taskOutputRepository;
	private final PackageRepository packageRepository;
	private final BlockRepository blockRepository;
	private final LocalShellJobLaucher localShellJobLaucher;
	private final NotificationService notificationService;
	@Autowired
	public JobService(JobRepository jobRepository, TaskRepository taskRepository,
			TaskStatusRepository taskStatusRepository, TaskCpuStatsRepository taskCpuStatsRepository,
			TaskMemoryStatsRepository taskMemoryStatsRepository, TaskOutputRepository taskOutputRepository,
			PackageRepository packageRepository, BlockRepository blockRepository, NotificationService notificationService) {
		this.jobRepository = jobRepository;
		this.taskRepository = taskRepository;
		this.taskStatusRepository = taskStatusRepository;
		this.taskCpuStatsRepository = taskCpuStatsRepository;
		this.taskMemoryStatsRepository = taskMemoryStatsRepository;
		this.taskOutputRepository = taskOutputRepository;
		this.packageRepository = packageRepository;
		this.blockRepository = blockRepository;
		this.notificationService=notificationService;

		localShellJobLaucher = new LocalShellJobLaucher(
				DynamicExecutors.newListeningDynamicScalingThreadPool("local-shell-job-executors"));
		localShellJobLaucher.registerListener(this);
	}

	@Transactional
	public JobStatus create(final ApplicationDescriptor job) {
		if (isNullOrEmpty(job.getId())) {
			job.setId(randomUUID().toString());
		}

		JobStatus jobStatus = new JobStatus(job.getId(), job.getName());

		jobRepository.insert(job.setCreatedIn(now(UTC).toEpochMilli()));

		checkAndFixBlocksStates(job);

		List<Tree<Step>> applicationsExecutionTrees = createApplicationsExecutionDependencyTrees(job.applications(),
				jobStatus);
		List<Tree<BlockAdapter>> blocksExecutionTrees = createBlocksExecutionDependencyTrees(job.blocks(), jobStatus);

		job.getBlocks().forEach(block -> createApplications(block.getApplications()));

		createApplications(job.applications());
		createTaskStatuses(jobStatus.statuses());
		blockRepository.insert(job.getBlocks());

		localShellJobLaucher.submitJobs(applicationsExecutionTrees);
		localShellJobLaucher.submitBlocksToExecution(blocksExecutionTrees);

		return jobStatus;
	}

	private List<Tree<BlockAdapter>> createBlocksExecutionDependencyTrees(Iterable<Block> blocks, JobStatus jobStatus) {
		Map<String, Tree<BlockAdapter>> trees = new HashMap<>();

		blocks.forEach(block -> {
			List<Tree<Step>> blockApps = createApplicationsExecutionDependencyTrees(block.applications(), jobStatus);

			Preconditions.checkState(!blockApps.isEmpty() && blockApps.size() == 1,
					"Block tree execution has more than one root!");

			TreeNode<BlockAdapter> node = new TreeNode<BlockAdapter>(new BlockAdapter(block, blockApps.get(0)));
			if (!block.hasParents()) {
				trees.put(block.name(), new Tree<BlockAdapter>(node));
			} else {
				TreeNode<BlockAdapter> parent = null;
				Iterator<String> iter = block.getParents().iterator();

				while (parent == null && iter.hasNext()) {
					String name = iter.next();

					treef: for (Tree<BlockAdapter> tree : trees.values()) {
						for (TreeNode<BlockAdapter> tn : tree.build(TreeTraversalOrderType.PRE_ORDER)) {
							if (name.equalsIgnoreCase(tn.getData().getBlock().name())) {
								parent = tn;
								break treef;
							}
						}
					}
				}

				assert parent != null;
				parent.addChild(new TreeNode<>(new BlockAdapter(block, blockApps.get(0))));
			}
		});

		return ImmutableList.copyOf(trees.values());
	}

	private void checkAndFixBlocksStates(ApplicationDescriptor job) {
		job.blocks().forEach(b -> {
			b.setJobId(job.getId());

			if (isNullOrEmpty(b.getId())) {
				b.setId(randomUUID().toString());
			}

			if (isNullOrEmpty(b.getName())) {
				b.setName(b.getId());
			}

			List<Application> applications = b.applications();
			int i = 0;

			do {
				Application application = applications.get(i);

				if (isNullOrEmpty(application.getId())) {
					application.setId(randomUUID().toString());
				}

				if (i - 1 >= 0) {
					application.addParent(applications.get(i - 1).getName());
				}

				application.setBlockId(b.getId());
				application.setJobId(b.getJobId());

			} while (++i < applications.size());

			b.setPlainText(new ObjectMapperUtil().toJson(b).orElse(null));
		});
	}

	protected Job configurePreconditions(JobStatus status, List<Precondition> preconditions) {
		Job job = new Job(format("preconditions-%s", status.getId()));

		preconditions.forEach(precondition -> {
			Flow flow = new Flow(format("preconditions-%s-flow", status.getId()));
			List<Package> packages = new ArrayList<>();

			precondition.getPackages().forEach(name -> {
				Optional<Package> pkg = packageRepository.findByName(name);

				if (pkg.isPresent()) {
					packages.add(pkg.get());
				}
			});

			flow.add(new Step(flow.getName(), flow.getName() + "packages", PackageManagerType.command(packages)));
			job.add(flow);
		});

		return job;
	}

	protected List<Tree<Step>> createApplicationsExecutionDependencyTrees(final Iterable<Application> applications,
			final JobStatus jobStatus) {
		Map<String, Tree<Step>> trees = new HashMap<>();

		applications.forEach(application -> {
			if (isNullOrEmpty(application.getId())) {
				application.setId(randomUUID().toString());
			}

			application.setJobId(jobStatus.getId());
			jobStatus.addTaskStatus(newPendingTaskStatus(application.getId(), application.getName()));

			Long timeout = application.getTimeout() == null
					? getLongProperty("org.excalibur.task.default.timeout", 3600L) : application.getTimeout();

			final Step step = new Step(application.getId(), application.getName(),
					newCommandBuilder().setId(application.getId()).setCommands("bash", "-c",
							String.format("runexec --output %s.log --walltimelimit %s -- %s; cat %s.log;  rm -f %s.log",
									application.getId(), timeout, application.getCommandLine(), application.getId(),
									application.getId()))
							.registerListeners(Collections.singletonList(JobService.this)));

			includeApplicationFilesHandler(application, step);

			TreeNode<Step> node = new TreeNode<>(step);

			if (!application.hasParents()) {
				trees.put(step.name(), new Tree<>(node));
			} else {
				TreeNode<Step> parent = null;
				Iterator<String> parents = application.parents().iterator();

				while (parents.hasNext() && parent == null) {
					String name = parents.next();

					t: for (Tree<Step> tree : trees.values()) {
						for (TreeNode<Step> n : tree.build(TreeTraversalOrderType.PRE_ORDER)) {
							if (n.getData().name().equalsIgnoreCase(name)) {
								parent = n;
								break t;
							}
						}
					}
				}

				assert parent != null;
				parent.addChild(node);
			}
		});

		return ImmutableList.copyOf(trees.values());
	}

	private void includeApplicationFilesHandler(Application application, Step step) {
		application.getFiles().forEach(f -> {
			String destPath = Files
					.simplifyPath(org.excalibur.core.io.utils.Files.expandHomePrefixReference(getFullPath(f.dest())));
			String dest = FilenameUtils.normalize(destPath.concat(File.separator))
					.concat(FilenameUtils.getName(f.dest()));

			step.action().addEnviromentVariable(f.name(), dest);

			java.util.Optional<URI> uri = f.getSourceURI();

			if (uri.isPresent() && uri.get().getScheme() != null) {
				switch (uri.get().getScheme()) {
				case "http":
				case "https":
					String commandline = format("mkdir -p %s && cd %s && %s %s && chmod +x *", destPath, destPath,
							getProperty("org.excalibur.default.network.downloader",
									format("wget --no-cookies --no-check-certificate -O %s", dest)),
							f.source().trim());

					step.addTaskLets(
							newBashCommand(randomUUID().toString(), commandline).excludeEnvironmentVariables());
					break;
				}
			}
		});
	}

	@Transactional
	public void createApplications(Iterable<Application> apps) {
		taskRepository.insert(apps);
	}

	@Transactional
	public void createTaskStatuses(Iterable<TaskStatus> statuses) {
		taskStatusRepository.insert(statuses);
	}

	@Subscribe
	public void createTaskStatus(final TaskStatus status) {
		if (status != null) {
			taskStatusRepository.insert(status);
		}
		
	}

	@Subscribe
	public void updateExecutionResult(TaskExecutionResult result) {
		if (result != null) {
		
			TaskOutput output = new TaskOutput().setTaskId(result.getId()).setId(randomUUID().toString())
					.setType(TaskOutputType.SYSOUT)
					.setValue(new String(Base64.encodeBase64(result.getOutput().getBytes())))
					.setChecksum(sha256().hashString(result.getOutput(), UTF_8).toString());

			try {
				taskOutputRepository.insert(output);
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@Subscribe
	public void notifyTaskOutput(TaskMessage message) {
		LOG.info("TaskName:{} TaskId: {} Status: {}",message.getTaskName(),message.getTaskId(),message.getStatus());
		if (message != null) {
			Application task=taskRepository.findByUUID(message.getTaskId());
			message.setJobId(task.getJobId());
			notificationService.notify(message);
		}
	}
	
	@Subscribe
	public void updateProcessState(ProcessState ps) {
		taskStatusRepository.updateTaskPid(ps.getId(), ps.getPid());

		if (ps.getCpuState() != null) {
			taskCpuStatsRepository.insert(ps.getCpuState());
		}

		if (ps.getMemoryState() != null) {
			ProcessMemoryState memory = ps.getMemoryState().clone()
					.setSize(ps.getMemoryState().getSize() / pow(1000, 2))
					.setResident(ps.getMemoryState().getResident() / pow(1000, 2))
					.setShare(ps.getMemoryState().getShare() / pow(1000, 2));

			taskMemoryStatsRepository.insert(memory);
		}
	}

	@Subscribe
	public void updateJobStatus(JobExecution jobExecution) {
		LOG.info("Job finished");
	}

	public Optional<TaskStatus> lastTaskStatus(String taskId) {
		return taskStatusRepository.getLastStatusOfTask(taskId);
	}

	public Optional<TaskStatus> lastTaskStatus(String jobId, String taskId) {
		return this.taskStatusRepository.getLastStatusOfTask(taskId);
	}

	public Optional<JobStatus> getJobTaskStatuses(String jobId) {
		Optional<JobStatus> result = Optional.absent();
		final Optional<ApplicationDescriptor> descriptor = jobRepository.findByUUID(jobId);

		if (descriptor.isPresent()) {
			final JobStatus js = new JobStatus().setId(jobId).setName(descriptor.get().getName());

			List<Application> tasks = taskRepository.findAllTasksOfJob(jobId);

			if (!tasks.isEmpty()) {
				tasks.forEach(t -> {
					Optional<TaskStatus> status = taskStatusRepository.getLastStatusOfTask(t.getId());
					js.addTaskStatus(status.orNull());
				});

				result = Optional.of(js);
			}
		}

		return result;
	}

	public Optional<TaskStats> getTaskStats(final String taskId) {
		Optional<TaskStats> stats = Optional.absent();

		if (!isNullOrEmpty(taskId)) {
			List<ProcessCpuState> cpu = taskCpuStatsRepository.getStatsOfTask(taskId);
			List<ProcessMemoryState> mem = taskMemoryStatsRepository.getStatsOfTask(taskId);

			if (!cpu.isEmpty() || !mem.isEmpty()) {
				stats = Optional.of(new TaskStats(taskId, cpu, mem));
			}
		}
		return stats;
	}

	public List<Application> getTasksOfJob(String jobId) {
		return ImmutableList.copyOf(taskRepository.findAllTasksOfJob(jobId));
	}

	public Optional<ApplicationDescriptor> getJob(String user, String jobId) {
		Optional<ApplicationDescriptor> job = jobRepository.findByUUID(jobId);

		if (job.isPresent()) {
			job.get().addApplications(getTasksOfJob(jobId));
		}

		return job;
	}

	public ImmutableList<TaskOutput> getTaskOutput(String jobId, String taskId) {
		return ImmutableList.copyOf(this.taskOutputRepository.getAllOutputsOfTask(taskId));
	}
}
