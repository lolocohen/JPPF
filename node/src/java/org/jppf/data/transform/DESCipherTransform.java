/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.data.transform;

import java.io.InputStream;
import java.security.spec.KeySpec;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.logging.*;
import org.jppf.utils.FileUtils;

/**
 * Sample data transform that uses the DES cyptographic algorithm with a 56 bits secret key. 
 * @author Laurent Cohen
 */
public class DESCipherTransform implements JPPFDataTransform
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(DESCipherTransform.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Secret (symetric) key used for encryption and decryption.
	 */
	private static SecretKey secretKey = getSecretKey();

	/**
	 * Encrypt the data.
	 * @param data the data to transform.
	 * @return the transformed data as an array of bytes.
	 * @see org.jppf.data.transform.JPPFDataTransform#wrap(byte[])
	 */
	public byte[] wrap(byte[] data)
	{
		byte[] result = null;
		try
		{
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			result = cipher.doFinal(data);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Decrypt the data.
	 * @param data the data to transform.
	 * @return the transformed data as an array of bytes.
	 * @see org.jppf.data.transform.JPPFDataTransform#unwrap(byte[])
	 */
	public byte[] unwrap(byte[] data)
	{
		byte[] result = null;
		try
		{
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			result = cipher.doFinal(data);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Get the secret key used for encryption/decryption.
	 * In this method, the secret key is read from a location in the classpath.
	 * This is defnitely unsecure, and for demonstration purposes only.
	 * The secret key should be stored in a secure location such as a key store.
	 * @return a <code>SecretKey</code> instance.
	 */
	private static synchronized SecretKey getSecretKey()
	{
		if (secretKey == null)
		{
			try
			{
				InputStream is = DESCipherTransform.class.getClassLoader().getResourceAsStream("org/jppf/data/transform/sk.bin");
				byte[] encoded = FileUtils.getInputStreamAsByte(is);
				KeySpec spec = new DESKeySpec(encoded);
				SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
				return skf.generateSecret(spec);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		return secretKey;
	}
}
