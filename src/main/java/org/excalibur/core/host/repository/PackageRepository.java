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
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.excalibur.core.host.repository.PackageRepository.BindPackage.PackageBinderFactory;
import org.excalibur.core.host.repository.PackageRepository.PackageRepositorySetMapper;
import org.excalibur.core.json.databind.ObjectMapperUtil;
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

import io.dohko.jdbi.stereotype.Repository;
import io.dohko.job.host.Platform;
import io.dohko.job.host.Package;

@Repository
@RegisterMapper(PackageRepositorySetMapper.class)
public interface PackageRepository extends Closeable 
{
	@GetGeneratedKeys
	@SqlUpdate("INSERT INTO package (name, version, architecture, description, platforms, dependencies) VALUES (:name, :version, :architecture, :description, :platforms, :dependencies)")
	Integer insert(@BindPackage Package pkg);
	
	@SqlUpdate("INSERT INTO package (name, version, architecture, description, platforms, dependencies) VALUES (:name, :version, :architecture, :description, :platforms, :dependencies)")
	void insert(@BindPackage  Iterable<Package> packages);
	
	@SingleValueResult
	@SqlQuery("SELECT name, version, architecture, description, platforms, dependencies FROM package WHERE lower(name) = lower(:name)")
	Optional<Package> findByName(@Bind("name") String name);
	
	@SqlQuery("SELECT name, version, architecture, description, platforms, dependencies FROM package ORDER BY name")
	List<Package> getAll();
	
	@org.skife.jdbi.v2.sqlobject.BindingAnnotation(PackageBinderFactory.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface BindPackage
    {
        public static class PackageBinderFactory implements BinderFactory
        {
            @Override
            public Binder<BindPackage, Package> build(Annotation annotation)
            {
                return new Binder<BindPackage, Package>()
                {
                    @Override
                    public void bind(SQLStatement<?> q, BindPackage bind, Package arg)
                    {
                        q.bind("name", arg.getName());
                        q.bind("version", arg.getVersion());
                        q.bind("architecture", arg.getArchitecture());
                        q.bind("description", arg.getDescription());
                        q.bind("platforms", new ObjectMapperUtil().writeValueAsString(arg.getPlatform()).orElse(null));
                        q.bind("dependencies", new ObjectMapperUtil().writeValueAsString(arg.getDependencies()).orElse(null));
                    }
                };
            }
        }
    }
	
	
	public class PackageRepositorySetMapper implements ResultSetMapper<Package> 
	{
		@Override
		public Package map(int index, ResultSet r, StatementContext ctx) throws SQLException 
		{
			return new Package()
					.setArchitecture(r.getString("architecture"))
					.setDescription(r.getString("description"))
					.setName(r.getString("name"))
					.setPlatform(new ObjectMapperUtil().readJsonValue(Platform.class, "platforms", r).orElse(new Platform()));
		}
	}
}
