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
package io.dohko.job;

import java.util.TimeZone;

import javax.inject.Inject;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;



@SpringBootApplication
@EnableSwagger2
@ImportResource({"classpath*:META-INF/applicationContext.xml"})
@EnableBatchProcessing
public class Application 
{
	static 
	{
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	
	@Inject
	Environment environment;
	
	public static void main(String[] args) 
	{
		SpringApplication app = new SpringApplication(Application.class);
		app.run(args);
	}
	
	/**
	 * Adds {@link GuavaModule} and {@link JaxbAnnotationModule} as Jackson's modules.
	 * This enables Spring to serialize and deserialize types annotatted with JAXB annotations
	 * and to work with Guava collection types such as ImmutableList.
	 * @return a jackson's builder with {@link GuavaModule} and {@link JaxbAnnotationModule} modules
	 */
	@Bean
	public Jackson2ObjectMapperBuilder objectMapperBuilder() 
	{
	    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
	    builder.serializationInclusion(JsonInclude.Include.NON_NULL)
	           .featuresToEnable(SerializationFeature.INDENT_OUTPUT)
	           .modules(new GuavaModule(), new JaxbAnnotationModule());
	    
	    return builder;
	}

	@Bean
	public Docket api() 
	{
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.basePackage("io.dohko.job.resource"))
				.paths(PathSelectors.any())
				.build()
				// .pathMapping("/")
				.tags(new Tag("Dohko Job Service", "All apis relating to Dohko's job scheduling"));
	}
	
//	@Bean
//	public UndertowEmbeddedServletContainerFactory servletContainer() 
//	{
//		UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
//		factory.addBuilderCustomizers(new UndertowBuilderCustomizer() 
//		{
//			@Override
//			public void customize(Builder builder) 
//			{
//				builder.addHttpListener(SystemUtils2.getIntegerProperty("org.excalibur.server.port", 9090), "0.0.0.0");
//			}
//		});
////		factory.setPort(SystemUtils2.getIntegerProperty("org.excalibur.server.port", 8080));
//		return factory;	
//	}
	
	
//	@Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) 
//	{
//        return args -> 
//        {
//
//            System.out.println("Let's inspect the beans provided by Spring Boot:");
//
//            String[] beanNames = ctx.getBeanDefinitionNames();
//            Arrays.sort(beanNames);
//            for (String beanName : beanNames) 
//            {
//                System.out.println(beanName);
//            }
//
//        };
//    }
}
