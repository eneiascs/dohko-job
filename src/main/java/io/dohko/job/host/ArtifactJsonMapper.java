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
package io.dohko.job.host;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.excalibur.core.io.utils.Files;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;


public class ArtifactJsonMapper 
{
	private final ObjectMapper mapper;
	
	public ArtifactJsonMapper()
	{
		mapper = new ObjectMapper();
		mapper.registerModules(new JaxbAnnotationModule(), new GuavaModule());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}
	
	
	public ImmutableList<Artifact> deserialize(String json) throws IOException
	{
		Preconditions.checkArgument(!Strings.isNullOrEmpty(json), "Json to deserialize is null or empty");
		Builder<Artifact> builder = ImmutableList.<Artifact>builder();
		
		@SuppressWarnings("unchecked")
		final HashMap<String, Object> types = mapper.readValue(json, java.util.HashMap.class);
        
        types.keySet().forEach(k -> 
        {
        	Map<String, Object> values = getValue(types, k, new HashMap<>());
        	
        	Artifact artifact = new Artifact()
        			.setAction(Action.valueOf(getValue(values, "action", "install").toUpperCase()))
        			.setChecksum(getString(values, "checksum"))
        			.setDescription(getString(values, "description"))
        			.setHashingMethod(values.containsKey("checksum_type") ? HashingMethod.valueOf(getValue(values, "checksum_type", "sha256").toUpperCase()) : null)
        			.setName(k)
        			.setVersion(getString(values, "version"));
        	
        	if (types.containsKey("mountpoint")) 
        	{
        		Map<String, Object> mountpoint = getValue(values, "mountpoint", new HashMap<String, Object>());
        		artifact.setMountpoint(new Mountpoint()
        				                     .setExportName(getString(mountpoint, "export_name"))
        				                     .setLocation(getString(mountpoint, "location"))
        				                     .setSudo(Boolean.valueOf(getValue(mountpoint, "sudo", "false"))));
        	}
        	
        	Map<String, Object> source = getValue(values, "source", new HashMap<>());
    		artifact.setSource(new Source().setUrl(getString(source, "url")));
    		getValue(source, "params", new ArrayList<String>()).forEach(artifact.getSource()::addParam);
        	
        	ArrayList<String> dependencies = getValue(values, "dependencies", new ArrayList<>());
        	dependencies.forEach(artifact::addDependency);
        	
        	builder.add(artifact);
        });
        
		return builder.build();
	}
	
	public ImmutableList<Artifact> deserialize(File input) throws IOException 
	{
        final String json = Joiner.on("\n").join(Files.readLines(input));
        return deserialize(json);
	}
	
	/**
	 * Serialize the given {@link List} as JSON.
	 * 
	 * @param artifacts {@link Artifact}s to be written as JSON
	 * @return
	 * @throws IOException
	 */
	public String serialize(final List<Artifact> artifacts) throws IOException
	{
		try (StringWriter writer = new StringWriter(); JsonGenerator jsonGenerator = createJsonGenerator(writer);)
		{
			jsonGenerator.writeStartObject();
			
			for (Artifact artifact: artifacts)
			{
				writeObject(jsonGenerator, artifact);
			}
			
			jsonGenerator.writeEndObject();
			jsonGenerator.flush();
			return writer.toString();
		}
	}
	
	public String serialize(Artifact artifact) throws IOException
	{
		try (StringWriter writer = new StringWriter(); JsonGenerator generator = createJsonGenerator(writer);)
		{
			generator.writeStartObject();
			writeObject(generator, artifact);
			generator.writeEndObject();
			
			generator.flush();
			
			return writer.toString();
		}
	}
	
	private JsonGenerator createJsonGenerator(Writer writer) throws IOException 
	{
		JsonGenerator jsonGenerator = new JsonFactory().createGenerator(writer);
		jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
		jsonGenerator.enable(Feature.STRICT_DUPLICATE_DETECTION);
		
		return jsonGenerator;
	}


	private void writeObject(JsonGenerator jsonGenerator, Artifact artifact) throws IOException 
	{
		jsonGenerator.writeFieldName(artifact.getName());
		jsonGenerator.writeStartObject();
		jsonGenerator.writeStringField("version", artifact.getVersion());
		
		if (artifact.getSource() != null)
		{
			jsonGenerator.writeObjectFieldStart("source");
			jsonGenerator.writeStringField("url", artifact.getSource().getUrl());
//			jsonGenerator.writeStringField("type", artifact.getSource().getMethod());
			
			if (!artifact.getSource().params().isEmpty())
			{
				jsonGenerator.writeArrayFieldStart("params");
				
				for (String param: artifact.getSource().params())
				{
					jsonGenerator.writeString(param);
				}
				
				jsonGenerator.writeEndArray();
			}
			
			jsonGenerator.writeEndObject();
		}
		
		
		jsonGenerator.writeStringField("action", artifact.getAction().name().toLowerCase());
		
		if (!Strings.isNullOrEmpty(artifact.getChecksum()))
		{
			jsonGenerator.writeStringField("checksum", artifact.getChecksum());
		}
		
		if (artifact.getHashingMethod() != null) 
		{
			jsonGenerator.writeStringField("checksum_type", artifact.getHashingMethod().name().toLowerCase());
		} 
		
		jsonGenerator.writeFieldName("dependencies");
		jsonGenerator.writeStartArray();
		
		for (String dependency : artifact.dependencies())
		{
			jsonGenerator.writeString(dependency);
		}
		
		jsonGenerator.writeEndArray();
		jsonGenerator.writeEndObject();
	}
	
	private String getString(Map<String, Object> values, String key) 
	{
		return getValue(values, key, null);
	}

	@SuppressWarnings("unchecked")
	private <T> T getValue(Map<String, Object> values, String key, T defaultValue)
	{
		T value = (T) values.get(key);
		return value == null || value.toString().trim().isEmpty() ? defaultValue : value;
	}
	
//	public static ImmutableList<Artifact> deserialize (String file) throws IOException 
//	{
//		return new ArtifactJsonMapper().deserialize(new File(file));
//	}

	public static String serializeIt(Artifact arg) 
	{
		return null;
	}
	
}
