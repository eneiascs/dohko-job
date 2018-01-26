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
package org.excalibur.core.host.repository;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.excalibur.core.host.repository.ArtifactRepository.ArtifactRepositorySetMapper;
import org.excalibur.core.host.repository.PackageRepository.BindPackage.PackageBinderFactory;
import org.excalibur.core.util.AnyThrow;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import io.dohko.jdbi.stereotype.Repository;
import io.dohko.job.host.Artifact;
import io.dohko.job.host.ArtifactJsonMapper;

import static java.lang.String.format;
import static com.google.common.base.Preconditions.checkState;
import static io.dohko.job.host.ArtifactJsonMapper.serializeIt;

@Repository
@RegisterMapper(ArtifactRepositorySetMapper.class)
public interface ArtifactRepository extends Closeable 
{
	@GetGeneratedKeys
	@SqlUpdate("INSERT INTO artifact(name, version, description) VALUES (:name, :version, :description)")
	Integer insert(@BindArtifact Artifact artifact);
	
	@SqlUpdate("INSERT INTO artifact(name, version, description) VALUES (:name, :version, :description)")
	void insert(@BindArtifact Iterable<Artifact> artifacts);
	
	@SingleValueResult
	@SqlQuery("SELECT name, version, description FROM artifact WHERE lower(name) = lower(:name)")
	Optional<Artifact> findByName(@Bind("name") String name);
	
	@SingleValueResult
	@SqlQuery("SELECT name, version, description FROM artifact WHERE lower(name) = lower(:name) AND lower(version) = lower(:version)")
	Optional<Artifact> getArtifact(@Bind("name") String name, @Bind("version") String version);
	
	@SqlQuery("SELECT name, version, description FROM artifact ORDER BY lower(name)")
	List<Artifact> getAll();
	
	@org.skife.jdbi.v2.sqlobject.BindingAnnotation(PackageBinderFactory.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
	@interface BindArtifact 
	{
		public static class ArtifactBinderFactory implements BinderFactory 
		{
			@Override
			public Binder<BindArtifact, Artifact> build(Annotation annotation) 
			{
				return new Binder<BindArtifact, Artifact>() 
				{
					@Override
					public void bind(SQLStatement<?> q, BindArtifact bind, Artifact arg) 
					{
						q.bind("name", arg.getName());
						q.bind("version", arg.getVersion());
						q.bind("description", serializeIt(arg));
					}
				};
			}
		}
	}
	
	class ArtifactRepositorySetMapper implements ResultSetMapper<Artifact>
	{
		@Override
		public Artifact map(int index, ResultSet r, StatementContext ctx) throws SQLException 
		{
			try 
			{
				ImmutableList<Artifact> artifacts = new ArtifactJsonMapper().deserialize(r.getString("description"));
				checkState(artifacts.size() == 1, format("There are %s artifact descriptions. Therefore, only one was expected.", artifacts.size()));
				
				return artifacts.get(0);
			} 
			catch (IOException e) 
			{
				AnyThrow.throwUncheked(e);
				return new Artifact().setName(r.getString("name")).setVersion(r.getString("version"));
			}
		}
	}
}
