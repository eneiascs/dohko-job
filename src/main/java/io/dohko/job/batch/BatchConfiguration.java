/**
 *     Copyright (C) 2013-2014  the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

//@Configuration
//@EnableBatchProcessing
public class BatchConfiguration 
{
//	public final JobBuilderFactory jobBuilderFactory = new JobBuilderFactory
//			(
//					new SimpleJobRepository
//					(
//							new MapJobInstanceDao(), 
//							new MapJobExecutionDao(), 
//							new MapStepExecutionDao(), 
//							new MapExecutionContextDao()
//					)
//			);
	
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
    public DataSource dataSource;

	// read pending task from the database
//	@Bean
    public FlatFileItemReader<Person> reader() 
	{
        FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
        reader.setResource(new ClassPathResource("sample-data.csv"));
        
        reader.setLineMapper(new DefaultLineMapper<Person>() 
        {{
            setLineTokenizer(new DelimitedLineTokenizer() {{ setNames(new String[] { "firstName", "lastName" }); }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{ setTargetType(Person.class); }});
        }});
        
        return reader;
    }
	
	@Bean
	public PersonItemProcessor processor() 
	{
		return new PersonItemProcessor();
	}
	
	
	@Bean
    public JdbcBatchItemWriter<Person> writer() 
	{
        JdbcBatchItemWriter<Person> writer = new JdbcBatchItemWriter<Person>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Person>());
        writer.setSql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)");
        writer.setDataSource(dataSource);
        return writer;
    }
	
	@Bean
    public Job importUserJob(JobCompletionNotificationListener listener) 
	{
        return jobBuilderFactory.get("importUserJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1())
                .end()
                .build();
    }
	
	@Bean
    public Step step1() 
	{
        return stepBuilderFactory.get("step1")
                .<Person, Person> chunk(10)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }
}
