/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.dist.commandline;

import java.io.*;
import java.util.*;

import org.jppf.server.protocol.*;

/**
 * This task lists the files in a specified directory of the node's host.
 * @author Laurent Cohen
 */
public class TestTask extends CommandLineTask
{
	/**
	 * Directory in which to list the files.
	 */
	private String number = null;
	/**
	 * Determines whether this task should run on a linux or windows host.
	 */
	private boolean linux = true;

	/**
	 * Initialize the script's parameters.
	 * @param number directory in which to list the files.
	 */
	public TestTask(String number)
	{
		this.number = number;
	}

	/**
	 * Execute the script.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			//setCommandList("cat", "etc/file |", "grep -s A", ">", "output-"+ number +".txt");
			/*
			setCommandList("cat", "etc/file", "|", "grep", "-s", "A", ">", "output-"+ number +".txt");
			StringBuilder sb = new StringBuilder();
			for (String cmd: this.getCommandList()) sb.append(cmd).append(' ');
			System.out.println("command to run: " + sb.toString());
			setCaptureOutput(true);
			launchProcess();
			FileLocation fileLoc = new FileLocation("output-" + number + ".txt");
			FileLocation tmp = new FileLocation("/tmp/somefolder/output-" + number + ".txt");
			fileLoc.copyTo(tmp);
			*/
			String outputPath = "/home/lcohen/Downloads/folder1/output.txt";
			setCommandList("/bin/sh", "-c", "ps -A|grep java > " + outputPath);
			//setCommandList("/bin/sh", "-c", "cat etc/file|grep -s A > " + "output-"+ number + ".txt");
			//setCommandList("/bin/sh", "-c", "cat /etc/file|grep -s A > " + outputPath);
			StringBuilder sb = new StringBuilder();
			for (String cmd: this.getCommandList()) sb.append(cmd).append(' ');
			System.out.println("command to run: " + sb);
			setCaptureOutput(true);
			launchProcess();
			FileLocation fileLoc = new FileLocation(outputPath);
			FileLocation tmp = new FileLocation("/home/lcohen/Downloads/folder2/output.txt");
			fileLoc.copyTo(tmp);

			InputStream is = fileLoc.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);	
			
			// Read in the File
			String line = "";
			List<String> file = new ArrayList<String>();
			List<String> bFile = new ArrayList<String>();
			while ((line = br.readLine()) != null) file.add(line);
			// "Process" the File
			for (int i = file.size()-1; i >= 0; i--) bFile.add(file.get(i));
			// "Write" out the File
			for (String l : file) System.out.println(l);

			setResult(getStandardOutput());
		}
		catch(Exception e)
		{
			setResult("an exception was raised: " +e);
			setException(e);
		}
		System.out.println("std output:\n" + getStandardOutput());
		System.out.println("err output:\n" + getErrorOutput());
	}
}
