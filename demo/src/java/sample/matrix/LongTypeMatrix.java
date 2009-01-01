/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.matrix;

import java.io.Serializable;
import java.util.Random;

/**
 * This class represents a square matrix of arbitrary size.
 * @author Laurent Cohen
 */
public class LongTypeMatrix implements Serializable
{
	/**
	 * The range of values for random values.
	 */
	private static final long RANDOM_RANGE = 1000000;
	/**
	 * The size of this matrix. The matrix contains size*size values.
	 */
	private int size = 0;
	/**
	 * The values in this matrix.
	 */
	private long[][] values = null;
	
	/**
	 * Initialize this amtrix with a specified size.
	 * @param newSize the size of this matrix.
	 */
	public LongTypeMatrix(int newSize)
	{
		this.size = newSize;
		values = new long[size][size];
	}
	
	/**
	 * Initialize this matrix with random values.
	 */
	public void assignRandomValues()
	{
		Random rand = new Random(System.currentTimeMillis());
		for (int i=0; i<values.length; i++)
		{
			for (int j=0; j<values[i].length; j++)
				// values in ]-RANDOM_RANGE, +RANDOM_RANGE[
				values[i][j] = 2 * rand.nextInt((int) RANDOM_RANGE + 1) - RANDOM_RANGE;
		}
	}

	/**
	 * Get the size of this matrix.
	 * @return the size as an integer value.
	 */
	public int getSize()
	{
		return size;
	}
	
	/**
	 * Get the row a matrix values at the specified index. Provided as a convenience.
	 * @param row the row index.
	 * @return the values in the row as an array of <code>double</code> values, or null if the row index is
	 * greater than the matrix size.
	 */
	public long[] getRow(int row)
	{
		return (row < size) ? values[row] : null;
	}
	
	/**
	 * Get a value at the specified coordinates.
	 * @param row the row coordinate.
	 * @param column the column coordinate.
	 * @return the specified value as a double.
	 */
	public double getValueAt(int row, int column)
	{
		return values[row][column];
	}
	
	/**
	 * Set a value to the specified coordinates.
	 * @param row the row coordinate.
	 * @param column the column coordinate.
	 * @param value the value to set.
	 */
	public void setValueAt(int row, int column, long value)
	{
		values[row][column] = value;
	}
	
	/**
	 * Compute the result of mutiplying this matrix by another: thisMatrix x otherMatrix.
	 * @param matrix the matrix to multiply this one by.
	 * @return a new matrix containing the reuslt of the multiplication.
	 */
	public LongTypeMatrix multiply(LongTypeMatrix matrix)
	{
		if (matrix.getSize() != size) return null;
		LongTypeMatrix result = new LongTypeMatrix(size);
		for (int i=0; i<size; i++)
		{
			for (int j=0; j<size; j++)
			{
				long value = 0;
				for (int k=0; k< size; k++) value += matrix.getValueAt(k, j) * values[i][k];
				result.setValueAt(j, i, value);
			}
		}
		return result;
	}

	/**
	 * Multiply a row of this matrix by another matrix.
	 * The result is a row in the resulting matrix multiplication.
	 * @param n the index of the row in this matrix.
	 * @param matrix the matrix to multiply by.
	 * @return a new row represented as an array of <code>double</code> values.
	 */
	public double[] multiplyRow(int n, LongTypeMatrix matrix)
	{
		double[] result = new double[size];
		for (int col=0; col<size; col++)
		{
			double sum = 0d;
			for (int row=0; row<size; row++)
			{
				sum += matrix.getValueAt(row, col) * getValueAt(n, row);
			}
			result[col] = sum;
		}
		return result;
	}
}
