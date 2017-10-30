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
package io.dohko.job;

import javax.inject.Inject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;



@SpringBootApplication(scanBasePackages = {})
@ImportResource({"classpath*:META-INF/applicationContext.xml"})
public class Application 
{
	@Inject
	Environment environment;
	
	public static void main(String[] args) 
	{
		SpringApplication app = new SpringApplication(Application.class);
		app.run(args);
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
