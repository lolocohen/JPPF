/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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