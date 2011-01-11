package com.dmr;

public class BPTC19696 {
	private boolean rawData[]=new boolean[196];
	private boolean deInterData[]=new boolean[196];
	private boolean outData[]=new boolean[96];
	
	// The main decode function
	public boolean decode (byte[] dibit_buf)	{
		boolean testOK;
		// Get the raw binary
		extractBinary(dibit_buf);
		// Deinterleave
		deInterleave();
		// Error check
		testOK=errorCheck();
		return testOK;
	}
	
	// Extract the binary from the dibit data
	private	void extractBinary (byte[] dibit_buf)	{
		int a,r=0;
		// First block
		for (a=12;a<61;a++)	{
			if (dibit_buf[a]==0)	{
				rawData[r]=false;
				rawData[r+1]=false;
			}
			else if (dibit_buf[a]==1)	{
				rawData[r]=false;
				rawData[r+1]=true;
			}
			else if (dibit_buf[a]==2)	{
				rawData[r]=true;
				rawData[r+1]=false;
			}
			else if (dibit_buf[a]==3)	{
				rawData[r]=true;
				rawData[r+1]=true;
			}
			r=r+2;
		}
		// Second block
		for (a=95;a<144;a++)	{
			if (dibit_buf[a]==0)	{
				rawData[r]=false;
				rawData[r+1]=false;
			}
			else if (dibit_buf[a]==1)	{
				rawData[r]=false;
				rawData[r+1]=true;
			}
			else if (dibit_buf[a]==2)	{
				rawData[r]=true;
				rawData[r+1]=false;
			}
			else if (dibit_buf[a]==3)	{
				rawData[r]=true;
				rawData[r+1]=true;
			}
			r=r+2;
		}
	}
	
	// Deinterleave the raw data
	private void deInterleave ()	{
		int a,interleaveSequence,pos=0;
		for (a=0;a<195;a++)	{
			// Calculate the interleave sequence
			interleaveSequence=(a*13)%196;
			// Shuffle the data
			deInterData[pos]=rawData[interleaveSequence];
			// Data fills the array in columns
			pos=pos+15;
			if (pos>194) pos=pos-194;
		}
	}
	
	// Check each row with a Hamming (15,11,3) code
	// Return false if there is a problem
	private boolean errorCheck ()	{
		int a;
		boolean row[]=new boolean[15];
		// First row
		for (a=0;a<15;a++)	{
			row[a]=deInterData[a];
		}
		if (hamming15113(row)==false) return false;
		
	return true;
	}
	
	// Hamming (15,11,3) check a boolean data array
	private boolean hamming15113 (boolean d[])	{
		boolean c[]=new boolean[4];
		// Calculate the checksum this row should have
		c[0]=d[0]^d[1]^d[2]^d[3]^d[5]^d[7]^d[8];
		c[1]=d[1]^d[2]^d[3]^d[4]^d[6]^d[8]^d[9];
		c[2]=d[2]^d[3]^d[4]^d[5]^d[7]^d[9]^d[10];
		c[3]=d[0]^d[1]^d[2]^d[4]^d[6]^d[7]^d[10];
		// Compare these with the actual bits
		if (c[0]!=d[11]) return false;
		else if (c[1]!=d[12]) return false;
		else if (c[2]!=d[13]) return false;
		else if (c[3]!=d[14]) return false;
		else return true;
	}

}
