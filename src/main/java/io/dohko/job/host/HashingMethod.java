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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Objects;
import com.google.common.io.Files;

import static com.google.common.hash.Hashing.md5;
import static com.google.common.hash.Hashing.sha256;
import static java.nio.charset.StandardCharsets.UTF_8;


@XmlType(name = "hashing-method")
@XmlEnum(String.class)
public enum HashingMethod 
{
	@XmlEnumValue("sha256")
	SHA256 
	{
		@Override
		public String hash(String input) 
		{
			return sha256().hashString(input, UTF_8).toString();
		}

		@Override
		public boolean isEquals(File input, String checksum) throws IOException 
		{
			return Objects.equal(checksum, Files.asByteSource(input).hash(sha256()).toString());
		}
	},
	
	@XmlEnumValue("md5")
	MD5 
	{
		@SuppressWarnings("deprecation")
		@Override
		public String hash(String input) 
		{
			return md5().hashString(input, UTF_8).toString();
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean isEquals(File input, String checksum) throws IOException
		{
			return Objects.equal(checksum, Files.asByteSource(input).hash(md5()).toString()); 
		}
	};
	
	public abstract String hash(String input);
	
	public abstract boolean isEquals(File input, String checksum) throws IOException;
}
