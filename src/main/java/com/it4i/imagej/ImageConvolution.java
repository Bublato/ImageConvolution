package com.it4i.imagej;
/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;


import mpi.MPI;
import mpi.MPIException;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.NumberWidget;


import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs, and
 * replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>ImageConvolution")
public class ImageConvolution<T extends RealType<T>> implements Command, Previewable {

	@Parameter
	private LogService ls;

	@Parameter
	private UIService uiService;

	@Parameter(type = ItemIO.INPUT)
	private Dataset inputImg;

	@Parameter(label = "Kernel", persist = false, style = NumberWidget.SLIDER_STYLE, min = "1", max = "18", stepSize = "1.0")
	private double kernel;

	public void preview() {
		switch((int)kernel) {
		  case 1:
			  ls.info("Box blur 3x3 selected.");
		    break;
		  case 2:
			  ls.info("Box blur 5x5 selected.");
		  break;
		  case 3:
			  ls.info("Edge dectection 1 selected.");
		    break;
		  case 4:
			  ls.info("Edge dectection 2 selected.");
		    break;
		  case 5:
			  ls.info("Edge dectection 3 selected.");
		    break;
		  case 6:
			  ls.info("Sharpness filter selected.");
		    break;
		  case 7:
			  ls.info("Gaussian blur 3x3 selected.");
		    break;
		  case 8:
			  ls.info("Gaussian blur 5x5 selected.");
		    break;
		  case 9:			  
			  ls.info("Gaussian blur 7x7 selected.");
		    break;
		  case 10:
			  ls.info("Gaussian blur 9x9 selected.");
		    break;
		  case 11:
			  ls.info("Gaussian blur 11x11 selected.");
		    break;
		  case 12:
			  ls.info("Gaussian blur 13x13 selected.");
		    break;
		  case 13:
			  ls.info("Gaussian blur 15x15 selected.");
		    break;
		  case 14:
			  ls.info("Gaussian blur 17x17 selected.");
		    break;
		  case 15:
			  ls.info("Gaussian blur 19x19 selected.");
		    break;
		  case 16:
			  ls.info("Gaussian blur 21x21 selected.");
		    break;
		  case 17:
			  ls.info("Gaussian blur 51x51 selected.");
		    break;
		  case 18:
			  ls.info("Gaussian blur 101x101 selected.");
		    break;
		}		
	}

	public void cancel() {
		
	}

	@Override
	public void run() {		

		final Img<T> image = (Img<T>)inputImg.getImgPlus();
        System.out.println("numDimensions = " + image.numDimensions());
        
        String bitdepth = inputImg.getTypeLabelShort();
        System.out.println("Pixel type = '" + bitdepth + "'");
        
        if(!bitdepth.equals("8-bit uint"))
        {
        	System.out.println("Image is not 8-bit. Program will terminate.");
        	return;
        }
        
        RandomAccess<T> ra = image.randomAccess();
        boolean isSingle = false;
        int zStack = 0;
        long dimensions[] = new long[image.numDimensions()];
        image.dimensions(dimensions);
        if(dimensions.length == 2){
        	zStack = 1;
        	isSingle = true;}
        if(dimensions.length == 3){
        	zStack = (int)dimensions[2];}
        
        
        for (int i = 0; i < image.numDimensions(); i++) {
        	System.out.println(i + " = " + dimensions[i]);
        }
        int[] out = new int[(int)(dimensions[0] * dimensions[1] * zStack)];
        int inc = 0;
        if(isSingle){
	    	for (int x = 0; x < dimensions[1]; x++) {
	    		for (int y = 0; y < dimensions[0]; y++) {
	    			ra.setPosition(x, 1);
	    			ra.setPosition(y, 0);        			
	    			T val = ra.get();
	    			out[inc] = (int)val.getRealDouble();
	    			inc++;
	    		}
	    	}     
        } else {
        	for (int c = 0; c < zStack; c++) {
            	for (int x = 0; x < dimensions[1]; x++) {
            		for (int y = 0; y< dimensions[0]; y++) {
            			ra.setPosition(x, 1);
            			ra.setPosition(y, 0);  
            			ra.setPosition(c, 2);
            			T val = ra.get();
            			out[inc] = (int)val.getRealDouble();
            			inc++;
            		}
            	}
            }        
        }       
        
        saveImgStack(out, "temp.raw");
		try {
			convolve("temp.raw", (int)dimensions[0], (int)dimensions[1], zStack, (int)kernel);
		} catch (MPIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Convolution completed.");
	}
	
	public static IntBuffer getArrFromRaw(File f, int col, int row, int slice)
	{
		FileInputStream fileInputStream = null;
        byte[] fileContent = null;

        try {
            fileContent = new byte[(int) f.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(f);
            fileInputStream.read(fileContent);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        
        IntBuffer out = MPI.newIntBuffer(col*row);
        int slicePos = slice*col*row;
        
		for(int i = 0; i < col*row; i++)
		{
			int i2 = (fileContent[slicePos + i] & 0xFF);
			out.put(i,i2);
		}
		return out;
	}
	
	public static void saveImgStack(int[] inputArray, String name)
	{
		int bufSize = inputArray.length;
		byte[] buf = new byte[bufSize];
		for(int i=0;i<bufSize;i++){
			buf[i] = (byte)inputArray[i];
		}
		
		FileOutputStream fileOuputStream = null;

        try {
            fileOuputStream = new FileOutputStream(name);
            fileOuputStream.write(buf);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOuputStream != null) {
                try {
                    fileOuputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	public static void saveImg(IntBuffer inputArray, String name)
	{
		int bufSize = inputArray.capacity();
		byte[] buf = new byte[bufSize];
		for(int i=0;i<bufSize;i++){
			buf[i] = (byte)inputArray.get(i);
		}
		
		FileOutputStream fileOuputStream = null;

        try {
            fileOuputStream = new FileOutputStream(name);
            fileOuputStream.write(buf);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOuputStream != null) {
                try {
                    fileOuputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	private static IntBuffer twoDconvolutionFastVer(IntBuffer inputArray, int columns, int rows, int[][] kernel, long denom, IntBuffer up, IntBuffer down)
	{
		int retSize = inputArray.capacity();
		IntBuffer ret = MPI.newIntBuffer(retSize);
		int kenDim = kernel.length;
		int[] vectorX = new int[kenDim];
		int[] vectorY = new int[kenDim];
		int acc = 0;		
		int positionIndex = 0;
		int mirrorY = 0; 
		int mirrorPositionIndex = 0;
		
		for(int i = 0; i < kenDim; i++)
		{
			vectorX[i] = kernel[i][0];
			vectorY[i] = kernel[0][i];
		}
		
		/*if(down != null)
		{
			for(int i = 0; i < kenDim/2; i++)
			{
				for(int j = 0; j < 20; j++)
				{
					System.out.print(down.get(i*columns+j) + ";");
				}
				System.out.println();
			}
		}*/
		//The core of convolution calculation. We omit the edges as there are many conditions that slows the algorithm. 
		for(int row = kenDim/2; row < rows - kenDim/2; row++) {		
			for(int col = 0; col < columns; col++)
			{
				//Initialize accumulator
				acc = 0;		
				//Cycle through kernel and use its values to fill the accumulator.
				for(int y = 0; y < kenDim; y++) {			
					positionIndex = ((row + y - (kenDim / 2)) * columns) + col;					
					acc += vectorY[y] * inputArray.get(positionIndex);
				}
				
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer				
				ret.put((row * columns) + col,acc);
			}
		}
		
		//Top edge handling with corners
		//Cycle through all bottom rows that belong to kernel overlay
		for(int row = 0; row < kenDim/2; row++){
			for(int col = 0; col < columns; col++) {
				acc = 0;
				//Cycle through kernel
				for(int y = 0; y < kenDim; y++) {		
					positionIndex = ((row + y - (kenDim / 2)) * columns) + col;
					//If requested pixel is inside frame
					if(positionIndex >= 0)
					{
						acc += vectorY[y] * inputArray.get(positionIndex);								
					}
					//If requested pixel is outside the frame
					else {
						//Check for empty receive buffer. If empty, use mirror it.
						if(up == null)
						{
							mirrorY = (kenDim -1) - y; 
							mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + col;
							acc += vectorY[y] * inputArray.get(mirrorPositionIndex);
						}
						else
						{		
							acc += vectorY[y] * up.get(positionIndex + (columns * (kenDim/2)));
							//acc += vectorY[y] * inputArray.get(row * columns + col);
						}						
					}					
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}
		}
		
		//Bottom edge handling without corners
		//Cycle through all bottom rows that belong to kernel overlay
		for(int row = rows - kenDim/2; row < rows; row++){
			for(int col = 0; col < columns; col++) {
				acc = 0;
				//Cycle through kernel
				for(int y = 0; y < kenDim; y++) {	
					positionIndex = ((row + y - (kenDim / 2)) * columns) + col;
					//If requested pixel is inside frame
					if(positionIndex <= rows*columns-1)
					{
						acc += vectorY[y] * inputArray.get(positionIndex);
					}
					//If requested pixel is outside the frame
					else {
						//Check for empty receive buffer. If empty, use default(center pixel) value
						if(down == null)
						{
							mirrorY = (kenDim -1) - y; 
							mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + col;
							acc += vectorY[y] * inputArray.get(mirrorPositionIndex);
						}
						else
						{
							acc += vectorY[y] * down.get(positionIndex - rows * columns);
						}						
					}					
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		return ret;
	}

	public static IntBuffer twoDconvolutionFastHor(IntBuffer inputArray, int columns, int rows, int[][] kernel, long denom, IntBuffer up, IntBuffer down)
	{
		int retSize = inputArray.capacity();
		IntBuffer ret = MPI.newIntBuffer(retSize);
		int kenDim = kernel.length;
		int[] vectorX = new int[kenDim];
		int[] vectorY = new int[kenDim];
		int acc = 0;		
		int positionIndex = 0;
		int mirrorX = 0; 
		int mirrorPositionIndex = 0;
		IntBuffer retUp = null;
		IntBuffer retDown = null;
		if(up != null)
		{
			retUp = MPI.newIntBuffer(up.capacity());
			
	    	
		}
		if(down != null)
		{
			retDown = MPI.newIntBuffer(down.capacity());
			
			/*for(int i = 0; i < kenDim/2; i++)
    		{
    			for(int j = 0; j < 20; j++)
    			{
    				System.out.print(down.get(i*columns+j) + ",");
    			}
    			System.out.println();
    		}*/
		}
		
		//Check the symmetry and kernel size
		if(!isOddAndSym(kernel))
		{
			System.out.println("Invalid kernel");
			return ret;
		}
		for(int i = 0; i < kenDim; i++)
		{
			vectorX[i] = kernel[i][0];			
			vectorY[i] = kernel[0][i];
		}
		//The core of convolution calculation. We omit the edges as there are many conditions that slows the algorithm. 
		for(int row = 0; row < rows; row++) {		
			for(int col = kenDim/2; col < columns-(kenDim/2); col++)
			{
				//Initialize accumulator
				acc = 0;		
				//Cycle through kernel and use its values to fill the accumulator.
				for(int x = 0; x < kenDim; x++) {			
					positionIndex = (row  * columns) + (col + x - (kenDim / 2));					
					acc += vectorX[x] * inputArray.get(positionIndex);
				}
				
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				
				ret.put((row * columns) + col,acc);
			}
		}
		
		//Left edge handling
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < kenDim/2; col++) {
				acc = 0;
				for(int x = 0; x < kenDim; x++) {			
					positionIndex = (row * columns) + (col + x - (kenDim / 2));
					//If posIndex is inside the image
					if(positionIndex >= 0)
					{
						//If posIndex is NOT on the other side of the image.	
						if(positionIndex >= (row * columns))
						{
							acc += vectorX[x] * inputArray.get(positionIndex);
						}
						//If posIndex IS on the other side of the image, mirror it. 
						else
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * inputArray.get(mirrorPositionIndex);
						}
					}
					else //if outside the image, mirror it
					{
						mirrorX = (kenDim -1) - x; 
						mirrorPositionIndex = (row  * columns) + (col + mirrorX - (kenDim / 2));
						acc += vectorX[x] * inputArray.get(mirrorPositionIndex);
					}
				}
				
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
				
				
		//Right edge handling
		for(int row = 0; row < rows; row++){
			for(int col = columns - kenDim/2; col < columns; col++) {
				acc = 0;
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows*columns-1)
						{
							//If posIndex is NOT on the other side of the image
							if(positionIndex < (row * columns) + columns)
							{
								acc += vectorX[x] * inputArray.get(positionIndex);
							}
							//if posIndex is on the left side of core pixel, wrap the value from other side of the image
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
								acc += vectorX[x] * inputArray.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, wrap the value
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * inputArray.get(mirrorPositionIndex);
						}
					}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		
		//The core of top buffer calculation. We omit the edges as there are many conditions that slows the algorithm. 
		if(up != null) {
			for(int row = 0; row < kenDim/2; row++) {			
				for(int col = kenDim/2; col < columns-(kenDim/2); col++)
				{
					//Initialize accumulator
					acc = 0;		
					//Cycle through kernel and use its values to fill the accumulator.
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row  * columns) + (col + x - (kenDim / 2));					
						acc += vectorX[x] * up.get(positionIndex);
					}
					
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					
					//Insert to return buffer
					retUp.put((row * columns) + col,acc);
				}
			}
		
			//Top buffer left edge handling
			for(int row = 0; row < kenDim/2; row++){
				for(int col = 0; col < kenDim/2; col++) {
					acc = 0;
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex >= 0)
						{
							//If posIndex is NOT on the other side of the image.	
							if(positionIndex >= (row * columns))
							{
								acc += vectorX[x] * up.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
								acc += vectorX[x] * up.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, mirror it
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row  * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * up.get(mirrorPositionIndex);
						}
					}
					
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					
					//Insert to return buffer
					retUp.put((row * columns) + col,acc);
				}			
			}
	
			//Top buffer right edge handling
			for(int row = 0; row < kenDim/2; row++){
				for(int col = columns - kenDim/2; col < columns; col++) {
					acc = 0;
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows*columns-1)
						{
							//If posIndex is NOT on the other side of the image
							if(positionIndex < (row * columns) + columns)
							{
								acc += vectorX[x] * up.get(positionIndex);
							}
							//if posIndex is on the left side of core pixel, wrap the value from other side of the image
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
								acc += vectorX[x] * up.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, wrap the value
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * up.get(mirrorPositionIndex);
						}
					}
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					//Insert to return buffer
					retUp.put((row * columns) + col,acc);
				}			
			}
		}
		
		
		//The core of down buffer calculation. We omit the edges as there are many conditions that slows the algorithm. 
		if(down != null) {
			for(int row = 0; row < kenDim/2; row++) {			
				for(int col = kenDim/2; col < columns-(kenDim/2); col++)
				{
					//Initialize accumulator
					acc = 0;		
					//Cycle through kernel and use its values to fill the accumulator.
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row  * columns) + (col + x - (kenDim / 2));					
						acc += vectorX[x] * down.get(positionIndex);
					}
					
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					
					//Insert to return buffer
					retDown.put((row * columns) + col,acc);
				}
			}
			//Down buffer left edge handling
			for(int row = 0; row < kenDim/2; row++){
				for(int col = 0; col < kenDim/2; col++) {
					acc = 0;
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex >= 0)
						{
							//If posIndex is NOT on the other side of the image.	
							if(positionIndex >= (row * columns))
							{
								acc += vectorX[x] * down.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
								acc += vectorX[x] * down.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, mirror it
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row  * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * down.get(mirrorPositionIndex);
						}
					}
					
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					
					//Insert to return buffer
					retDown.put((row * columns) + col,acc);
				}			
			}
	
			//Down buffer right edge handling
			for(int row = 0; row < kenDim/2; row++){
				for(int col = columns - kenDim/2; col < columns; col++) {
					acc = 0;
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = (row * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows*columns-1)
						{
							//If posIndex is NOT on the other side of the image
							if(positionIndex < (row * columns) + columns)
							{
								acc += vectorX[x] * down.get(positionIndex);
							}
							//if posIndex is on the left side of core pixel, wrap the value from other side of the image
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
								acc += vectorX[x] * down.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, wrap the value
						{
							mirrorX = (kenDim -1) - x; 
							mirrorPositionIndex = (row * columns) + (col + mirrorX - (kenDim / 2));
							acc += vectorX[x] * down.get(mirrorPositionIndex);
						}
					}
					
					//Use denominator
					if(denom != 0)
					{
						acc /= denom;
					}
					
					//Normalize to 8bit pixels
					if(acc > 255)
					{
						acc = 255;
					}
					if(acc < 0)
					{
						acc = 0;
					}
					//Insert to return buffer
					retDown.put((row * columns) + col,acc);
				}			
			}
		}
    			
		ret = twoDconvolutionFastVer(ret, columns, rows, kernel, denom, retUp, retDown);
		return ret;
	}
	
	
	public static IntBuffer twoDconvolution(IntBuffer inputArray, int columns, int rows, int[][] kernel, long denom, IntBuffer up, IntBuffer down)
	{
		int retSize = inputArray.capacity();
		IntBuffer ret = MPI.newIntBuffer(retSize);
		int kenDim = kernel.length;	
		int acc = 0;	
		int positionIndex = 0;
		int mirrorX = 0; 
		int mirrorY = 0;
		int mirrorPositionIndex = 0;
		int mirrorXPositionIndex = 0;
		
		//Check the symmetry and kernel size
		if(!isOddAndSym(kernel))
		{
			System.out.println("Invalid kernel");
			return ret;
		}	
		
		//The core of convolution calculation. We omit the edges as there are many conditions that slows the algorithm. 
		for(int row = kenDim/2; row < rows-(kenDim/2); row++) {			
			for(int col = kenDim/2; col < columns-(kenDim/2); col++){
				//Initialize accumulator
				acc = 0;						
				//Cycle through kernel and use its values to fill the accumulator.
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {	
						//Calculate the position of pixel in image at which the kernel position points.
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));	
						//Add subsum to accumulator
						acc += kernel[x][y] * inputArray.get(positionIndex);
					}
				}
				//Use denominator
				if(denom != 0){
					acc /= denom;
				}
				//Normalize to 8bit pixels
				if(acc > 255){
					acc = 255;
				}
				if(acc < 0)	{
					acc = 0;
				}
				//Insert to return buffer				
				ret.put((row * columns) + col,acc);
			}
		}
			
		//Handling edges with recv buffers
		
		//Top edge handling without corners
		//Cycle through all bottom rows that belong to kernel overlay
		for(int row = 0; row < kenDim/2; row++){
			for(int col = kenDim/2; col < columns-kenDim/2; col++) {
				acc = 0;
				//Cycle through kernel
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If requested pixel is inside frame
						if(positionIndex >= 0)
						{
							acc += kernel[x][y] * inputArray.get(positionIndex);								
						}
						//If requested pixel is outside the frame
						else {
							//Check for empty receive buffer. If empty, use mirror it.
							if(up == null)
							{
								mirrorX = (kenDim -1) - x; 
								mirrorY = (kenDim -1) - y; 
								mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
							else
							{		
								acc += kernel[x][y] * up.get(positionIndex + (columns * (kenDim/2)));
								//acc += kernel[x][y] * inputArray.get((row*columns) + col);
							}						
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}
		}
		
		//Bottom edge handling without corners
		//Cycle through all bottom rows that belong to kernel overlay
		for(int row = rows - kenDim/2; row < rows; row++){
			for(int col = kenDim/2; col < columns-kenDim/2; col++) {
				acc = 0;
				//Cycle through kernel
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If requested pixel is inside frame
						if(positionIndex <= rows*columns-1)
						{
							acc += kernel[x][y] * inputArray.get(positionIndex);
						}
						//If requested pixel is outside the frame
						else {
							//Check for empty receive buffer. If empty, use default(center pixel) value
							if(down == null)
							{
								mirrorX = (kenDim -1) - x; 
								mirrorY = (kenDim -1) - y; 
								mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
							else
							{
								acc += kernel[x][y] * down.get(positionIndex - rows * columns);
							}						
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		//Left edge handling
		for(int row = kenDim/2; row < rows-kenDim/2; row++){
			for(int col = 0; col < kenDim/2; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex >= 0)
						{
							//If posIndex is NOT on the other side of the image.	
							if(positionIndex >= (row + (y - kenDim/2)) * columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorY = (kenDim -1) - y; 
								mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, mirror it
						{
							mirrorX = (kenDim -1) - x; 
							mirrorY = (kenDim -1) - y; 
							mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
							acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		
		//Right edge handling
		for(int row = kenDim/2; row < rows-kenDim/2; row++){
			for(int col = columns - kenDim/2; col < columns; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows*columns-1)
						{
							//If posIndex is NOT on the other side of the image
							if(positionIndex < ((row + (y-kenDim/2)) * columns) + columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//if posIndex is on the left side of core pixel, wrap the value from other side of the image
							else
							{
								mirrorX = (kenDim -1) - x; 
								mirrorY = (kenDim -1) - y; 
								mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
						else //if outside the image, wrap the value
						{
							mirrorX = (kenDim -1) - x; 
							mirrorY = (kenDim -1) - y; 
							mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
							acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		//Corner pixel handling
		
		//Top left corner
		for(int row = 0; row < kenDim/2; row++){
			for(int col = 0; col < kenDim/2; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex >= 0)
						{
							//If posIndex is NOT on the other side of the image.
							if(positionIndex >= (row + (y - kenDim/2)) * columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								//acc += kernel[x][y] * inputArray.get(row*columns + col);
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								//Check whether the calculated mirror position is still in the image.
								if(mirrorPositionIndex < 0)
								{
									//Check for empty receive Buffer. If empty, use default(center pixel) value
									if(up == null)
									{		
										 mirrorXPositionIndex = ((row + y - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
										
										acc += kernel[x][y] * inputArray.get(mirrorXPositionIndex);
									}
									else
									{								
										acc += kernel[x][y] * up.get(mirrorPositionIndex + columns * ((kenDim/2)));
									}						
								}
								else
								{
									acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
								}
							}
						}
						else //if outside the image
						{
							//If posIndex is NOT on the other side of the image.
							if(positionIndex >= (row + (y - kenDim/2)) * columns)
							{
								//Check for empty receive buffer. If empty, use default(center pixel) value
								if(up == null)
								{
									acc += kernel[x][y] * inputArray.get((row*columns) + col);
								}
								else
								{								
									acc += kernel[x][y] * up.get(positionIndex + (columns * (kenDim/2)));
								}						
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		//Top right corner
		for(int row = 0; row < kenDim/2; row++){
			for(int col = columns - kenDim/2; col < columns; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex >= 0)
						{
							//If posIndex is NOT on the left side of the image
							if(positionIndex < ((row + (y-kenDim/2)) * columns) + columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//if posIndex is on the left side of core pixel, wrap the value from other side of the image
							else
							{
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								//Check whether the calculated mirror position is still in the image.
								if(mirrorPositionIndex < 0)
								{
									//Check for empty receive Buffer. If empty, use default(center pixel) value
									if(up == null)
									{										
										 mirrorXPositionIndex = ((row + y - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));										
										acc += kernel[x][y] * inputArray.get(mirrorXPositionIndex);
									}
									else
									{								
										acc += kernel[x][y] * up.get(mirrorPositionIndex + columns * ((kenDim/2)));
									}						
								}
								else
								{
									acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
								}
							}
						}
						else //if outside the image, wrap the value
						{
							//If posIndex is NOT on the left side of the image
							if(positionIndex < ((row + (y-kenDim/2)) * columns) + columns)
							{
								//Check for empty receive buffer. If empty, use default(center pixel) value
								if(up == null)
								{
									acc += kernel[x][y] * inputArray.get((row*columns) + col);
								}
								else
								{								
									acc += kernel[x][y] * up.get(positionIndex + (columns * (kenDim/2)));
								}
							}
							else
							{
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}
		
		//Bottom left corner
		for(int row = rows - kenDim/2; row < rows; row++){
			for(int col = 0; col < kenDim/2; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows * columns - 1)
						{
							//If posIndex is NOT on the other side of the image.
							if(positionIndex >= (row + (y - kenDim/2)) * columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								//acc += kernel[x][y] * inputArray.get(row*columns + col);
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								//Check whether the calculated mirror position is still in the image.
								if(mirrorPositionIndex > rows * columns - 1)
								{
									//Check for empty receive buffer. If empty, use default(center pixel) value
									if(down == null)
									{										
										 mirrorXPositionIndex = ((row + y - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));										
										acc += kernel[x][y] * inputArray.get(mirrorXPositionIndex);
									}
									else
									{								
										acc += kernel[x][y] * down.get(mirrorPositionIndex - rows * columns);
									}						
								}
								else
								{
									acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
								}
							}
						}
						else //if outside the image, wrap the value
						{
							//If posIndex is NOT on the other side of the image.
							if(positionIndex >= (row + (y - kenDim/2)) * columns)
							{
								//Check for empty receive buffer. If empty, use default(center pixel) value
								if(down == null)
								{
									acc += kernel[x][y] * inputArray.get((row*columns) + col);
								}
								else
								{								
									acc += kernel[x][y] * down.get(positionIndex - rows * columns);
								}
							}
							else
							{
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}		
		
		//Bottom right corner
		for(int row = rows - kenDim/2; row < rows; row++){
			for(int col = columns - kenDim/2; col < columns; col++) {
				acc = 0;
				for(int y = 0; y < kenDim; y++) {
					for(int x = 0; x < kenDim; x++) {			
						positionIndex = ((row + y - (kenDim / 2)) * columns) + (col + x - (kenDim / 2));
						//If posIndex is inside the image
						if(positionIndex <= rows * columns - 1)
						{
							//If posIndex is NOT on the other side of the image use the value from posIndex. 
							if(positionIndex < ((row + (y-kenDim/2)) * columns) + columns)
							{
								acc += kernel[x][y] * inputArray.get(positionIndex);
							}
							//If posIndex IS on the other side of the image, mirror it. 
							else
							{
								//acc += kernel[x][y] * inputArray.get(row*columns + col);
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								//Check whether the calculated mirror position is still in the image.
								if(mirrorPositionIndex > rows * columns - 1)
								{
									//Check for empty receive buffer. If empty, use default(center pixel) value
									if(down == null)
									{										
										 mirrorXPositionIndex = ((row + y - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));										
										acc += kernel[x][y] * inputArray.get(mirrorXPositionIndex);
									}
									else
									{								
										acc += kernel[x][y] * down.get(mirrorPositionIndex - rows * columns);
									}						
								}
								else
								{
									acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
								}
							}
						}
						else //If outside the image
						{
							//If posIndex is NOT on the other side of the image.
							if(positionIndex < ((row + (y-kenDim/2)) * columns) + columns)
							{
								//Check for empty receive buffer. If empty, use default(center pixel) value
								if(down == null)
								{
									acc += kernel[x][y] * inputArray.get((row*columns) + col);
								}
								else
								{								
									acc += kernel[x][y] * down.get(positionIndex - rows * columns);
								}
							}
							else
							{
								 mirrorX = (kenDim -1) - x; 
								 mirrorY = (kenDim -1) - y; 
								 mirrorPositionIndex = ((row + mirrorY - (kenDim / 2)) * columns) + (col + mirrorX - (kenDim / 2));
								acc += kernel[x][y] * inputArray.get(mirrorPositionIndex);
							}
						}
					}
				}
				//Use denominator
				if(denom != 0)
				{
					acc /= denom;
				}
				
				//Normalize to 8bit pixels
				if(acc > 255)
				{
					acc = 255;
				}
				if(acc < 0)
				{
					acc = 0;
				}
				//Insert to return buffer
				ret.put((row * columns) + col,acc);
			}			
		}		
		return ret;
	}
	
	//Functions takes four parameters. Whole image, original image dimensions and number of nodes.
	//Function returns padded image that can be divided evenly between all nodes.
	public static IntBuffer padImage(IntBuffer inputArray, int columns, int rows, int world_size){
		int finRows = rows, finCols = columns;		
		
		if(rows % world_size != 0)
		{
			finRows = rows + (world_size - (rows % world_size));
		}
		
		if(columns % world_size != 0)
		{
			finCols = columns + (world_size - (columns % world_size));
		}		
		
		IntBuffer out = MPI.newIntBuffer(finRows * finCols);
		
		for(int row = 0; row < finRows; row++)
		{
			for(int col = 0; col < finCols; col++)
			{
				if(row >= rows) {				
					if(col >= columns) {
						out.put((row * finCols) + col, inputArray.get(((rows-1) * columns) + columns - 1));
					}
					else {
						out.put((row * finCols) + col,inputArray.get(((rows-1) * columns) + col));
					}
				}
				else
				{
					if(col >= columns) {
						out.put((row * finCols) + col, inputArray.get((row * (columns)) + columns - 1));
					}
					else {
						out.put((row * finCols) + col,inputArray.get((row * columns) + col));
					}
				}				
			}
		}		
		return out;
	}
	
	//Function takes three input parameters. Whole image and the dimensions. 
	//Function returns cropped image with the desired dimensions.
	public static IntBuffer cropImage(IntBuffer inputArray, int columns, int rows, int padding)
	{
		IntBuffer ret = MPI.newIntBuffer(rows*columns);
		int offset = 0;
		for (int i = 0; i < rows; i++)
		{
			for(int j = 0; j < columns; j++)
			{				
				ret.put(i*columns+j, inputArray.get(i*columns+j + offset));
				if(j == columns - 1)
				{
					offset += padding;
				}
			}
		}
		
		return ret;
	}
	
	//Function checks the symmetry and size of the kernel. Size needs to be odd as we need kernel with clearly defined center. 
	public static boolean isOddAndSym(int[][] kernel)
	{
		int kenDim = kernel.length;
		
		if(kenDim % 2 != 1)
		{
			return false;
		}
		else
		{
			for(int kernelRow = 0; kernelRow < kenDim; kernelRow++)
			{
				if(kernel[kernelRow].length != kenDim)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	//Function takes kernel as a parameter and returns sum of the values in the kernel.
	public static long getDenom(int[][] kernel)
	{
		long retVal = 0;
		if(!isOddAndSym(kernel))
		{
			System.out.println("Invalid kernel!");
			return -1;
		}
		else
		{
			for(int i = 0; i < kernel.length; i++)
			{
				for(int j = 0; j < kernel.length; j++)
				{
					retVal += kernel[i][j];
				}
			}
				
			return retVal;
		}		
	}
	
	public static int[][] createBoxKernel(int size)
	{
		int[][] ret = new int[size][size];
		
		for(int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++)
			{
				ret[i][j] = 1;
			}
		}
		return ret;
	}
	
	public static int[][] createKernel(int size)
	{
		if(size % 2 == 0)
		{
			size++;
		}
		
		
		int[][] ret = new int[size][size];
		int[] vec = new int[size];
		vec[0] = 1;
		vec[size-1] = 1;		
		int intensity = 2;
		
	    for (int i = 1; i <= (size-2)/2; i++) {
	    	vec[i] = i * intensity;
	    }
	    int acc = size/2;
	    for (int i = ((size)/2); i < size-1 ; i++) {
	    	vec[i] = acc * intensity;
	    	acc--;
	    }
	    
	    for (int i = 0; i < size; ++i) {
	        for (int j = 0; j < size; ++j) {
        		ret[i][j] = vec[i] * vec[j];
	        }
	    }
		return ret;
	}
	
    // function for exchanging two rows 
    // of a matrix  
    static void swap(int mat[][],  
          int row1, int row2, int col) 
    { 
        for (int i = 0; i < col; i++) 
        { 
            int temp = mat[row1][i]; 
            mat[row1][i] = mat[row2][i]; 
            mat[row2][i] = temp; 
        } 
    } 
      
    // Function to display a matrix 
    static void display(int mat[][],  
                     int row, int col) 
    { 
        for (int i = 0; i < row; i++) 
        { 
              
            for (int j = 0; j < col; j++) 
              
                System.out.print(" "
                          + mat[i][j]); 
                            
            System.out.print("\n"); 
        } 
    }  
      
    // function for finding rank of matrix  
    public static int rankOfMatrix(int matrix[][]) 
    {        
    	int[][] mat = new int[matrix.length][matrix.length];
    	
    	for(int i = 0; i < matrix.length; i++)
    		mat[i] = (int[]) matrix[i].clone();
    	
        int rank = mat.length; 
        int R = rank;
      
        for (int row = 0; row < rank; row++) 
        { 
            // Diagonal element is not zero 
            if (mat[row][row] != 0) 
            { 
                for (int col = 0; col < R; col++) 
                { 
                    if (col != row) 
                    { 
                        double mult =  
                           (double)mat[col][row] / 
                                    mat[row][row]; 
                                      
                        for (int i = 0; i < rank; i++) 
                          
                            mat[col][i] -= mult  
                                       * mat[row][i]; 
                    } 
                } 
            } 
            else
            { 
                boolean reduce = true; 
      
                // Find the non-zero element  
                // in current column  
                for (int i = row + 1; i < R; i++) 
                { 
                    // Swap the row with non-zero  
                    // element with this row. 
                    if (mat[i][row] != 0) 
                    { 
                        swap(mat, row, i, rank); 
                        reduce = false; 
                        break ; 
                    } 
                } 

                if (reduce) 
                { 
                    // Reduce number of columns 
                    rank--; 
      
                    // Copy the last column here 
                    for (int i = 0; i < R; i ++) 
                        mat[i][row] = mat[i][rank]; 
                } 
      
                // Process this row again 
                row--; 
            } 
        } 
          
        return rank; 
    } 
	
	public static void convolve(String filePath, int dimX, int dimY, int dimZ, int selectedKernel) throws MPIException, IOException, InterruptedException {
				
		String[] args = new String[5];
		args[0] = filePath;
		args[1] = Integer.toString(dimX);
		args[2] = Integer.toString(dimY);
		args[3] = Integer.toString(dimZ);
		args[4] = Integer.toString(selectedKernel);
		String[] res = new String[500];
		
		 if(!MPI.isInitialized())
		 {
			 MPI.Init(args);	
		 }
		
		long startTime = System.nanoTime();
		int world_rank = MPI.COMM_WORLD.getRank(),
        world_size = MPI.COMM_WORLD.getSize();
		int kernelNumber = 0;
		int numOfFrames = 1;
		int columns = 0;
		int rows = 0;
		File inputFile;		
		boolean isStack = false;
		if(args.length != 5)
		{
			System.out.println("Expected 5 parameters, got " + args.length + "!");
			MPI.Finalize();
			return;
		}
		else
		{
			inputFile = new File(args[0]);
			if(!inputFile.exists())
			{
				System.out.println("File in: " + args[0] + " does not exists!");
				MPI.Finalize();
				return;
			}
			
			try
		    {
		      // the String to int conversion happens here
				columns = Integer.parseInt(args[1].trim());
				rows = Integer.parseInt(args[2].trim());
				numOfFrames = Integer.parseInt(args[3].trim());
				kernelNumber = Integer.parseInt(args[4].trim());				
		    }
		    catch (NumberFormatException nfe)
		    {
		      System.out.println("NumberFormatException: " + nfe.getMessage());
		      System.out.println("Invalid dimensions. Format for input: path, dimX, dimY, slices, kernel.");
		      MPI.Finalize();
		      return;
		    }
			
			if(columns < 0 || rows < 0 || numOfFrames <= 0)
			{
				System.out.println("Invalid dimensions. Format for input: path, dimX, dimY, kernel");
				MPI.Finalize();
				return;
			}
			if(columns * rows * numOfFrames != inputFile.length())
			{
				System.out.println("File size does not match the input dimensions!");
				MPI.Finalize();
				return;
			}			
			
			if(numOfFrames > 1)
			{
				isStack = true;
			}
		}	
				
		//MPI.COMM_WORLD.barrier();
		//This section is for initializing the image size and kernels. 
		//It will be omitted later on as these variables will be input parameters.
		int padCol = columns;
		int padRow = rows;
		boolean hasDenom = false;
		int[][] kernel;	
		boolean isSeparable = false;
		
		//Gaus 3x3
		int[][] gaus3 = {{1,2,1}, {2,4,2}, {1,2,1}};
		//Gaus 5x5
		int[][] gaus5 = {{1,4,6,4,1}, {4,16,24,16,4}, {6,24,36,24,6}, {4,16,24,16,4}, {1,4,6,4,1}};
		//Identity
		int[][] identity = {{0,0,0}, {0,1,0}, {0,0,0}};
		//Edge detection 1
		int[][] edge1 = {{1,0,-1}, {0,0,0}, {-1,0,1}};
		//Edge detection 2
		int[][] edge2 = {{0,1,0}, {1,-4,1}, {0,1,0}};
		//Edge detection 3
		int[][] edge3 = {{-1,-1,-1}, {-1,8,-1}, {-1,-1,-1}};
		//Sharpen
		int[][] sharpen = {{0,-1,0}, {-1,5,-1}, {0,-1,0}};
		//Unsharp masking
		int[][] unsharp = {{1,4,6,4,1}, {4,16,24,16,4}, {6,24,-476,24,6}, {4,16,24,16,4}, {1,4,6,4,1}};
		
		//3x3 Box blur
		int[][] boxBlur3 = {{1,1,1}, {1,1,1}, {1,1,1}}; 
		
		//5x5 Box blur
		int[][] boxBlur5 = {{1,1,1,1,1}, {1,1,1,1,1}, {1,1,1,1,1}, {1,1,1,1,1}, {1,1,1,1,1}}; 
		
		//7x7 box blur
		int[][] boxBlur7 = {{1,1,1,1,1,1,1}, {1,1,1,1,1,1,1}, {1,1,1,1,1,1,1}, {1,1,1,1,1,1,1}, {1,1,1,1,1,1,1},{1,1,1,1,1,1,1},{1,1,1,1,1,1,1}}; 
		
		//9x9 box blur
		int[][] boxBlur9 = {{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1}}; 
		
		//11x11 box blur
		int[][] boxBlur11 = {{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1}};
		
		//13x13 box blur
		int[][] boxBlur13 = {{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1}}; 
		
		//15x15 box blur
		int[][] boxBlur15 = {{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}}; 
		
		//17x17 box blur
		int[][] boxBlur17 = {{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}}; 
		
		//19x19 box blur
		int[][] boxBlur19 = {{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}}; 
		
		//21x21 box blur
		int[][] boxBlur21 = {{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}}; 
		switch(kernelNumber) {
		  case 1:
			  kernel = boxBlur3;
			  hasDenom = true;
		    break;
		  case 2:
			  kernel = boxBlur5;
			  hasDenom = true;
		  break;
		  case 3:
			  kernel = edge1;
			  hasDenom = false;
		    break;
		  case 4:
			  kernel = edge2;
			  hasDenom = false;
		    break;
		  case 5:
			  kernel = edge3;
			  hasDenom = false;
		    break;
		  case 6:
			  kernel = sharpen;
			  hasDenom = false;
		    break;
		  case 7:
			  kernel = createKernel(3);
			  hasDenom = true;
		    break;
		  case 8:
			  kernel = createKernel(5);			  
			  hasDenom = true;
		    break;
		  case 9:			  
			  kernel = createKernel(7);			  			  
			  hasDenom = true;
		    break;
		  case 10:
			  kernel = createKernel(9);			  
			  hasDenom = true;
		    break;
		  case 11:
				  kernel = createKernel(11);			  
			  hasDenom = true;
		    break;
		  case 12:
				  kernel = createKernel(13);			  
			  hasDenom = true;
		    break;
		  case 13:
				  kernel = createKernel(15);			  
			  hasDenom = true;
		    break;
		  case 14:
				  kernel = createKernel(17);			  
			  hasDenom = true;
		    break;
		  case 15:
				  kernel = createKernel(19);			  
			  hasDenom = true;
		    break;
		  case 16:
				  kernel = createKernel(21);			  
			  hasDenom = true;
		    break;
		  case 17:
				  kernel = createKernel(51);			  
			  hasDenom = true;
		    break;
		  case 18:
				  kernel = createKernel(101);			  
			  hasDenom = true;
		    break;
		  default:
			  kernel = gaus3;
			  hasDenom = true;
		}
		
		long denom = 0;
		//Get denominator for the matrix if it has one.
		if(hasDenom)
		{
			denom = getDenom(kernel);
		}
		
		if(rankOfMatrix(kernel) == 1)
		{
			isSeparable = true;
		}
		
		
		//Calculate how many lines/columns will be omitted
		int overlay = kernel.length / 2;
	    
	    //Calculate how many padding columns or rows needs to be added to original image
	    //in order for the image to be divided evenly between nodes
		if(rows % world_size != 0)
		{
			padRow = rows + (world_size - (rows % world_size));
		}
		
		if(columns % world_size != 0)
		{
			padCol = columns + (world_size - (columns % world_size));
		}	
		//How many elements there will be for each process.
		int elements_per_proc = (padCol * padRow)/world_size;  	
		
		if(overlay > (padRow/world_size)/2)
		{
			if(world_rank == 0)
			{
				System.out.println("Kernel too big!");
			}			
			MPI.Finalize();
			return;
		}
		
		
		//Initialize in and out buffers. 
		IntBuffer ret = MPI.newIntBuffer(rows*columns);
    	IntBuffer in = MPI.newIntBuffer(padCol*padRow);
    	IntBuffer out = MPI.newIntBuffer(elements_per_proc);
    	//Initialize recv/send buffers. These will be used for sending/receiving extra rows between processes.
    	IntBuffer recvBufUp = MPI.newIntBuffer(padCol*overlay);
    	IntBuffer recvBufDown = MPI.newIntBuffer(padCol*overlay);
    	int[] toBeSaved = new int[rows*columns*numOfFrames];    	
    	
    	for(int frameNum = 0; frameNum < numOfFrames; frameNum++)
    	{	    	
		    if (world_rank == 0) {	
		    	//Gets an image from .raw and pads it into desired size.
	    		in = padImage(getArrFromRaw(inputFile,columns, rows, frameNum),columns,rows,world_size);
		    }
	
		    //Wait for all nodes to reach this point.
		    //MPI.COMM_WORLD.barrier();
		  
		    //Divide the input image evenly between all the nodes
		    MPI.COMM_WORLD.scatter(in, elements_per_proc, MPI.INT, out,
		            elements_per_proc, MPI.INT, 0);
		    
		    //Wait for all nodes to reach this point.
		    //MPI.COMM_WORLD.barrier();	    
		    
		    //In this section we handle send/recv buffer sending between processes.
		    //First node is treaded differently as it will not receive buffer from above itself.
		    if(world_rank == 0)
		    {
		    	int lastRowIndex = ((elements_per_proc/padCol) - overlay) * padCol;
		    	IntBuffer sendBuf = MPI.newIntBuffer(padCol*overlay);
		    	sendBuf.clear();
		    	int x = 0;
		    	//Fill the send buffer with last overlaying values from node 0 part of the image. 
		    	for(int i = lastRowIndex; i < lastRowIndex+(padCol*overlay); i++)
		    	{
		    		sendBuf.put(x,out.get(i));
		    		x++;
		    	}
		    	
		    	//Check whether we are NOT the only node in the program. 
		    	//If we aren't we proceed with receiving and sending buffers.
		    	if(world_size > 1)
		    	{	    		
		    		MPI.COMM_WORLD.recv(recvBufDown, padCol*overlay, MPI.INT, world_rank+1, 99);
		    		MPI.COMM_WORLD.send(sendBuf, padCol*overlay, MPI.INT, world_rank+1, 99);	
		    		//As this is node 0, it won't receive any buffer above itself. 
		    		recvBufUp = null;
		    	}
		    	else
		    	{
		    		recvBufUp = null;
		    		recvBufDown = null;
		    	}
		    }
		    //Last node is also treated differently as it won't receive any buffers from below itself.
		    else if(world_rank == world_size - 1)
		    {
		    	IntBuffer sendBuf = MPI.newIntBuffer(padCol*overlay);
		    	//Fill the send buffer with first overlaying values from last node's part of the image. 
		    	for(int i = 0; i < padCol*overlay; i++)
		    	{
		    		sendBuf.put(out.get(i));
		    	}
		    	
		    	//Check whether we are NOT the only node in the program. 
		    	//If we aren't we proceed with receiving and sending buffers.
		    	if(world_size > 1)
		    	{
		    		//In order to avoid deadlock, odd nodes are first receiving and then sending the buffer. 
		    		//Odd nodes are first sending the buffers and then receiving.
		    		if(world_rank % 2 == 0){
		    			MPI.COMM_WORLD.recv(recvBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);
			    		MPI.COMM_WORLD.send(sendBuf, padCol*overlay, MPI.INT, world_rank-1, 99);	  		
			    	}else {
			    		MPI.COMM_WORLD.send(sendBuf, padCol*overlay, MPI.INT, world_rank-1, 99);
			    		MPI.COMM_WORLD.recv(recvBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);
			    	}
		    		recvBufDown = null;
		    	}
		    	
		    }
		    //Here we handle nodes between first and last node. Each of these are sharing 2 buffers between others.
		    else
		    {
		    	int lastRowIndex = ((elements_per_proc/padCol) - overlay) * padCol;
		    	IntBuffer sendBufUp = MPI.newIntBuffer(padCol*overlay);
		    	IntBuffer sendBufDown = MPI.newIntBuffer(padCol*overlay);
		    	//First we fill the up send buffer, then the down send buffer. 
		    	for(int i = 0; i < padCol*overlay; i++)
		    	{
		    		sendBufUp.put(out.get(i));
		    	}
		    	int x = 0;
		    	for(int i = lastRowIndex; i < lastRowIndex+(padCol*overlay); i++)
		    	{
		    		sendBufDown.put(x,out.get(i));
		    		x++;
		    	}
		    	//In order to avoid deadlock, odd nodes are first receiving and then sending the buffer. 
	    		//Odd nodes are first sending the buffers and then receiving.
		    	if(world_rank % 2 == 0)
		    	{
		    		MPI.COMM_WORLD.recv(recvBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);
			    	MPI.COMM_WORLD.recv(recvBufDown, padCol*overlay, MPI.INT, world_rank + 1, 99);
			    	MPI.COMM_WORLD.send(sendBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);	
			    	MPI.COMM_WORLD.send(sendBufDown, padCol*overlay, MPI.INT, world_rank+1, 99);
		    	}
		    	else
		    	{	    		
		    		MPI.COMM_WORLD.send(sendBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);	
		    		MPI.COMM_WORLD.send(sendBufDown, padCol*overlay, MPI.INT, world_rank+1, 99);
		    		MPI.COMM_WORLD.recv(recvBufUp, padCol*overlay, MPI.INT, world_rank-1, 99);
		    		MPI.COMM_WORLD.recv(recvBufDown, padCol*overlay, MPI.INT, world_rank + 1, 99);
		    	}	    	
		    }
		  
		    //Wait for all nodes to reach this point.
		    MPI.COMM_WORLD.barrier();		    
		    
		    //Each node calls convolution function for its own part of the image. 
		    //For separable matrixes we can call fast convolution. Separable matrix is  amatrix with rank 1.
		    //out = twoDconvolution(out, padCol, padRow/world_size, kernel, denom, recvBufUp, recvBufDown);

		    if(isSeparable) {
		    	if(world_rank == 0)
		    	{
		    		System.out.println("Using fast convolution.");
		    	}		    	
		    	out = twoDconvolutionFastHor(out, padCol, padRow/world_size, kernel, (long)Math.sqrt(denom), recvBufUp, recvBufDown);
		    }
		    else
		    {
		    	if(world_rank == 0)
		    	{
		    		System.out.println("Using slow convolution.");
		    	}	
		    	out = twoDconvolution(out, padCol, padRow/world_size, kernel, denom, recvBufUp, recvBufDown);
		    }
		    //Wait for all nodes to reach this point.
		    //MPI.COMM_WORLD.barrier();
		    //Gather all the convolved parts of the image into single image.
		    MPI.COMM_WORLD.gather(out, elements_per_proc, MPI.INT, in, elements_per_proc, MPI.INT, 0);
		    
		    //MPI.COMM_WORLD.barrier();
		    if(world_rank == 0)
		    {
		    	//Return the image from padded size into original size.
		    	ret = cropImage(in, columns, rows, padCol - columns);
		    	
		    	if(isStack == true)
		    	{
		    		int framePos = rows*columns*frameNum;		    	
			    	for(int i = 0; i < ret.capacity(); i++)
			    	{
			    		toBeSaved[framePos + i] = ret.get(i);
			    	}		    	
		    	}
		    	else
		    	{
		    		saveImg(ret, "result.raw");
		    	}
		    	
		    }	    
    	}
    	
	    if(world_rank == 0 && isStack == true)
	    {
	    	//Save image
	    	saveImgStack(toBeSaved, "result.raw");
	    }
	    //MPI.COMM_WORLD.barrier();
	    long elapsedTime = System.nanoTime() - startTime;
	    if(world_rank == 0)
	    {
	    	System.out.println("Total execution time for kernel " + kernel.length + "x" + kernel.length + " in Java in millis: "
	                + elapsedTime/1000000);
	    }    	       
	    //Finalize MPI and end. 		    
	}

	/**
	 * This main function serves for development purposes. It allows you to run
	 * the plugin immediately out of your integrated development environment
	 * (IDE).
	 *
	 * @param args
	 *            whatever, it's ignored
	 * @throws Exception
	 */
	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		
		// ask the user for a file to open
		final File file = ij.ui().chooseFile(null, "open");

		if (file != null) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

			// show the image
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(ImageConvolution.class, true);
		}

	}

}
