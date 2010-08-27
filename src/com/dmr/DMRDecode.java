// Please note much of the code in this program was taken from the DSD software
// and converted into Java. The author of this software is unknown but has the
// GPG Key ID below

// Copyright (C) 2010 DSD Author
// GPG Key ID: 0x3F1D7FD0 (74EF 430D F7F2 0A48 FCE6  F630 FAA2 635D 3F1D 7FD0)
// 
// Permission to use, copy, modify, and/or distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH
// REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT,
// INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
// LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE
// OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
// PERFORMANCE OF THIS SOFTWARE.
//

package com.dmr;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.*;

public class DMRDecode {
	private DisplayModel display_model;
	private DisplayView display_view;
	private static DMRDecode theApp;
	static DisplayFrame window;
	public String program_version="DMR Decoder V0.00 Build 0";
	public TargetDataLine Line;
	public AudioFormat format;
	public int vertical_scrollbar_value=0;
	public int horizontal_scrollbar_value=0;
	private boolean audioReady=false;
	private static boolean RUNNING=true;
	private int have_sync=0;
	private int samplesPerSymbol=10;
	private int rf_mod=0;
	private int jitter=-1;
	private int symbolCenter=4;
	private int max=15000;
	private int min=-15000;
	private int center=0;
	private int lastsample=0;
	private int numflips=0;
	private int maxref=12000;
	private int minref=-12000;
	private int lastsynctype=-1;
	private int symboltiming=0;
	private int symbolcnt=0;
	private int sbuf[]=new int[128];
	private int sidx=0;
	private int ssize=36;
	private int mod_threshold=26;

	public static void main(String[] args) {
		theApp=new DMRDecode();
		SwingUtilities.invokeLater(new Runnable(){public void run(){theApp.createGUI();}});
		
		// Prepare the program //
		//theApp.prepare_variables();
		theApp.prepare_audio();
		
		while (RUNNING)	{
			if (theApp.audioReady==true) theApp.decode();
		}

		}
	
	// Setup the window //
	public void createGUI() {
		window=new DisplayFrame(program_version,this);
		Toolkit theKit=window.getToolkit();
		Dimension wndsize=theKit.getScreenSize();
		window.setBounds(wndsize.width/6,wndsize.height/6,2*wndsize.width/3,2*wndsize.height/3);
		window.addWindowListener(new WindowHandler());
		display_model=new DisplayModel();
		display_view=new DisplayView(this);
		display_model.addObserver(display_view);
		window.getContentPane().add(display_view,BorderLayout.CENTER);
		window.setVisible(true);
		}

	class WindowHandler extends WindowAdapter {
		public void windowClosing(WindowEvent e) {	
			}
		}

	public DisplayFrame getWindow()	{
		return window;	
		}

	public DisplayModel getModel() {
		return display_model;
		}

	public DisplayView getView() {
		return display_view;	
		}
	

	// Setup the audio interface
	  public void prepare_audio() {
		  try {
			  format = new AudioFormat(48000, 16, 1, true, true);
			  DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			  Line = (TargetDataLine) AudioSystem.getLine(info);
			  Line.open(format);
			  Line.start();
			  audioReady=true;
		  } catch (Exception e) {
			  JOptionPane.showMessageDialog(null, "Fatal error in prepare_audio","DMRdecoder", JOptionPane.ERROR_MESSAGE);
			  System.exit(0);
	   		}
	   	}
	  
	  // The main routine for decoding DMR data
	  public void decode()	{
	
	
	  }
	  
	  // This code lifted straight from the DSD source code and needs a lot of tidying
	  public int getSymbol()	{
		  int sample,i,sum=0,symbol,count=0;
		  for (i = 0; i < samplesPerSymbol; i++)
		    {
		      // timing control
		      if ((i == 0) && (have_sync == 0))
		        {
		          if (rf_mod == 1)
		            {
		              if ((jitter >= 0) && (jitter < symbolCenter))
		                {
		                  i++;          // fall back
		                }
		              else if ((jitter > symbolCenter) && (jitter < 10))
		                {
		                  i--;          // catch up
		                }
		            }
		          else if (rf_mod == 2)
		            {
		              if ((jitter >= symbolCenter - 1) && (jitter <= symbolCenter))
		                {
		                  i--;
		                }
		              else if ((jitter >= symbolCenter + 1) && (jitter <= symbolCenter + 2))
		                {
		                  i++;
		                }
		            }
		          else if (rf_mod == 0)
		            {
		              if ((jitter > 0) && (jitter <= symbolCenter))
		                {
		                  i--;          // catch up
		                }
		              else if ((jitter > symbolCenter) && (jitter < samplesPerSymbol))
		                {
		                  i++;          // fall back
		                }
		            }
		          jitter = -1;
		        }

			      sample=getAudio();
		      
		      
		      if ((sample > max) && (have_sync == 1) && (rf_mod == 0))
		        {
		          sample = max;
		        }
		      else if ((sample < min) && (have_sync == 1) && (rf_mod == 0))
		        {
		          sample = min;
		        }

		      if (sample > center)
		        {
		          if (lastsample < center)
		            {
		              numflips += 1;
		            }
		          if (sample > (maxref * 1.25))
		            {
		              if (lastsample < (maxref * 1.25))
		                {
		                  numflips += 1;
		                }
		              if ((jitter < 0) && (rf_mod == 1))
		                {               // first spike out of place
		                  jitter = i;
		                }
		              if ((symboltiming == 1) && (have_sync == 0) && (lastsynctype != -1))
		                {
		                  //printf ("O");
		                }
		            }
		          else
		            {
		              if ((symboltiming == 1) && (have_sync == 0) && (lastsynctype != -1))
		                {
		                  //printf ("+");
		                }
		              if ((jitter < 0) && (lastsample < center) && (rf_mod != 1))
		                {               // first transition edge
		                  jitter = i;
		                }
		            }
		        }
		      else
		        {                       // sample < 0
		          if (lastsample > center)
		            {
		              numflips += 1;
		            }
		          if (sample < (minref * 1.25))
		            {
		              if (lastsample > (minref * 1.25))
		                {
		                  numflips += 1;
		                }
		              if ((jitter < 0) && (rf_mod == 1))
		                {               // first spike out of place
		                  jitter = i;
		                }
		              if ((symboltiming == 1) && (have_sync == 0) && (lastsynctype != -1))
		                {
		                  //printf ("X");
		                }
		            }
		          else
		            {
		              if ((symboltiming == 1) && (have_sync == 0) && (lastsynctype != -1))
		                {
		                  //printf ("-");
		                }
		              if ((jitter < 0) && (lastsample > center) && (rf_mod != 1))
		                {               // first transition edge
		                  jitter = i;
		                }
		            }
		        }
		      if (samplesPerSymbol == 5)
		        {
		          if ((i >= 2) && (i <= 2))
		            {
		              sum += sample;
		              count++;
		            }
		        }
		      else
		        {
		          if (((i >= symbolCenter - 1) && (i <= symbolCenter + 2) && (rf_mod == 0)) || (((i == symbolCenter) || (i == symbolCenter + 1)) && (rf_mod != 0)))
		            {
		              sum += sample;
		              count++;
		            }
		        }
		      lastsample = sample;
		    }
		  symbol = (sum / count);

		  if ((symboltiming == 1) && (have_sync == 0) && (lastsynctype != -1))
		    {
		      if (jitter >= 0)
		        {
		          //printf (" %i\n", jitter);
		        }
		      else
		        {
		          //printf ("\n");
		        }
		    }

		  symbolcnt++;
		  return symbol;
	  }
	  
	  // Grab and return a single byte from the audio line
	  public int getAudio ()	{
		  int sample,count,total=0;
		  byte buffer[]=new byte[2];
		  try	{
			  while (total<1)	{
				  count=Line.read(buffer,0,2);
				  total=total+count;
			  	}
			  } catch (Exception e)	{
			  String err=e.getMessage();
			  JOptionPane.showMessageDialog(null,err,"DMRDecode", JOptionPane.ERROR_MESSAGE);
		  }
		  sample=buffer[0];
		  return sample;
	  }
	  
	  public int getFrameSync ()	{
	    /* detects frame sync and returns frame type
	     * 0 = +p25p1
	     * 1 = -p25p1
	     * 2 = +X2-TDMA (non inverted signal data frame)
	     * 3 = +X2-TDMA (inverted signal voice frame)
	     * 4 = -X2-TDMA (non inverted signal voice frame)
	     * 5 = -X2-TDMA (inverted signal data frame)
	     * 6 = +D-STAR
	     * 7 = -D-STAR
	     * 8 = +NXDN96
	     * 9 = -NXDN96
	     * 10 = +DMR (non inverted singlan data frame)
	     * 11 = -DMR (inverted signal voice frame)
	     * 12 = +DMR (non inverted signal voice frame)
	     * 13 = -DMR (inverted signal data frame)
	     * 14 = +ProVoice
	     * 15 = -ProVoice
	     */

	    int i, j, t, o, dibit, sync, symbol, synctest_pos, lastt;
	    int synctest[]=new int[25];
	    int synctest18[]=new int[19];
	    int synctest32[]=new int[33];
	    int modulation[]=new int[8];
	    //char *synctest_p;
	    int synctest_buf[]=new int [10240];
	    int synctest_p[]=new int [10240];
	    int synctest_p_counter=0;
	    int lmin, lmax, lidx;
	    int lbuf[]=new int[24]; 
	    int lbuf2[]=new int[24];
	    int lsum;
	    int spectrum[]=new int[64];

	    // detect frame sync
	    t = 0;
	    synctest[24] = 0;
	    synctest18[18] = 0;
	    synctest32[32] = 0;
	    synctest_pos = 0;
	    synctest_p=synctest_buf;
	   
	    sync = 0;
	    lmin = 0;
	    lmax = 0;
	    lidx = 0;
	    lastt = 0;
	    numflips = 0;

	    while (sync == 0)
	      {
	        t++;
	        symbol=getSymbol();
	        lbuf[lidx] = symbol;
	        sbuf[sidx] = symbol;
	        if (lidx == 23)
	          {
	            lidx = 0;
	          }
	        else
	          {
	            lidx++;
	          }
	        if (sidx == (ssize - 1))
	          {
	            sidx = 0;
	          }
	        else
	          {
	            sidx++;
	          }

	        if (lastt == 23)
	          {
	            lastt = 0;
	            if (numflips > mod_threshold)
	              {

	              }
	            else if (numflips > 18)
	              {

	              }
	            else
	              {

	              }
	            numflips = 0;
	          }
	        else
	          {
	            lastt++;
	          }

	        //determine dibit state
	        if (symbol > 0)
	          {
	            *dibit_buf_p = 1;
	            dibit_buf_p++;
	            dibit = 49;
	          }
	        else
	          {
	            *dibit_buf_p = 3;
	            dibit_buf_p++;
	            dibit = 51;
	          }

	        synctest_p[synctest_p_counter]=dibit;
	        if (t >= 24)
	          {
	            for (i = 0; i < 24; i++)
	              {
	                lbuf2[i] = lbuf[i];
	              }
	            qsort (lbuf2, 24, sizeof (int), comp);
	            lmin = (lbuf2[2] + lbuf2[3] + lbuf2[4]) / 3;
	            lmax = (lbuf2[21] + lbuf2[20] + lbuf2[19]) / 3;

	            if (rf_mod == 1)
	              {
	                minbuf[midx] = lmin;
	                maxbuf[midx] = lmax;
	                if (midx == (msize - 1))
	                  {
	                    midx = 0;
	                  }
	                else
	                  {
	                    midx++;
	                  }
	                lsum = 0;
	                for (i = 0; i < msize; i++)
	                  {
	                    lsum += minbuf[i];
	                  }
	                min = lsum / msize;
	                lsum = 0;
	                for (i = 0; i < msize; i++)
	                  {
	                    lsum += maxbuf[i];
	                  }
	                max = lsum / msize;
	                center = ((max) + (min)) / 2;
	                maxref = ((max) * 0.80);
	                minref = ((min) * 0.80);
	              }
	            else
	              {
	                maxref = max;
	                minref = min;
	              }

	            if (datascope == 1)
	              {
	                if (lidx == 0)
	                  {
	                    for (i = 0; i < 64; i++)
	                      {
	                        spectrum[i] = 0;
	                      }
	                    for (i = 0; i < 24; i++)
	                      {
	                        o = (lbuf2[i] + 32768) / 1024;
	                        spectrum[o]++;
	                      }
	                    if (symbolcnt > (4800 / scoperate))
	                      {
	                        symbolcnt = 0;
	                        //printf ("\n");
	                        //printf ("Demod mode:     %s                Nac:                     %4X\n", modulation, nac);
	                        //printf ("Frame Type:    %s        Talkgroup:            %7i\n", ftype, lasttg);
	                        //printf ("Frame Subtype: %s       Source:          %12i\n", fsubtype, lastsrc);
	                        //printf ("TDMA activity:  %s %s     Voice errors: %s\n", slot0light, slot1light, err_str);
	                        //printf ("+----------------------------------------------------------------+\n");
	                        for (i = 0; i < 10; i++)
	                          {
	                            //printf ("|");
	                            for (j = 0; j < 64; j++)
	                              {
	                                if (i == 0)
	                                  {
	                                    if ((j == ((min) + 32768) / 1024) || (j == ((max) + 32768) / 1024))
	                                      {
	                                        //printf ("#");
	                                      }
	                                    else if (j == (center + 32768) / 1024)
	                                      {
	                                        //printf ("!");
	                                      }
	                                    else
	                                      {
	                                        if (j == 32)
	                                          {
	                                            //printf ("|");
	                                          }
	                                        else
	                                          {
	                                            //printf (" ");
	                                          }
	                                      }
	                                  }
	                                else
	                                  {
	                                    if (spectrum[j] > 9 - i)
	                                      {
	                                        //printf ("*");
	                                      }
	                                    else
	                                      {
	                                        if (j == 32)
	                                          {
	                                            //printf ("|");
	                                          }
	                                        else
	                                          {
	                                           //printf (" ");
	                                          }
	                                      }
	                                  }
	                              }
	                            //printf ("|\n");
	                          }
	                        //printf ("+----------------------------------------------------------------+\n");
	                      }
	                  }
	              }

	            strncpy (synctest, (synctest_p - 23), 24);


	            if (frame_dmr == 1)
	              {
	                if (strcmp (synctest, DMR_DATA_SYNC) == 0)
	                  {
	                    carrier = 1;
	                    offset = synctest_pos;
	                    max = ((max) + (lmax)) / 2;
	                    min = ((min) + (lmin)) / 2;
	                    if (inverted_dmr == 0)
	                      {
	                        // data frame
	                        sprintf (ftype, " DMR         ");
	                        if (errorbars == 1)
	                          {
	                            //printFrameSync (opts, state, " +DMR      ", synctest_pos + 1, modulation);
	                          }
	                        lastsynctype = 10;
	                        return (10);
	                      }
	                    else
	                      {
	                        // inverted voice frame
	                        sprintf (ftype, " DMR         ");
	                        if (errorbars == 1)
	                          {
	                            //printFrameSync (opts, state, " -DMR      ", synctest_pos + 1, modulation);
	                          }
	                        if (lastsynctype != 11)
	                          {
	                            firstframe = 1;
	                          }
	                        lastsynctype = 11;
	                        return (11);
	                      }
	                  }
	                if (strcmp (synctest, DMR_VOICE_SYNC) == 0)
	                  {
	                    carrier = 1;
	                    offset = synctest_pos;
	                    max = ((max) + lmax) / 2;
	                    min = ((min) + lmin) / 2;
	                    if (inverted_dmr == 0)
	                      {
	                        // voice frame
	                        sprintf (ftype, " DMR         ");
	                        if (errorbars == 1)
	                          {
	                           // printFrameSync (opts, state, " +DMR      ", synctest_pos + 1, modulation);
	                          }
	                        if (lastsynctype != 12)
	                          {
	                            firstframe = 1;
	                          }
	                        lastsynctype = 12;
	                        return (12);
	                      }
	                    else
	                      {
	                        // inverted data frame
	                        sprintf (ftype, " DMR         ");
	                        if (errorbars == 1)
	                          {
	                            //printFrameSync (opts, state, " -DMR      ", synctest_pos + 1, modulation);
	                          }
	                        lastsynctype = 13;
	                        return (13);
	                      }
	                  }
	              }

	            if ((t == 24) && (lastsynctype != -1))
	              {
	              if ((lastsynctype == 11) && (strcmp (synctest, DMR_VOICE_SYNC) != 0))
	                  {
	                    carrier = 1;
	                    offset = synctest_pos;
	                    max = ((max) + lmax) / 2;
	                    min = ((min) + lmin) / 2;
	                    sprintf (ftype, "(DMR)        ");
	                    if (errorbars == 1)
	                      {
	                        //printFrameSync (opts, state, "(-DMR)     ", synctest_pos + 1, modulation);
	                      }
	                    lastsynctype = -1;
	                    return (11);
	                  }
	                else if ((lastsynctype == 12) && (strcmp (synctest, DMR_DATA_SYNC) != 0))
	                  {
	                    carrier = 1;
	                    offset = synctest_pos;
	                    max = ((max) + lmax) / 2;
	                    min = ((min) + lmin) / 2;
	                    sprintf (ftype, "(DMR)        ");
	                    if (errorbars == 1)
	                      {
	                        //printFrameSync (opts, state, "(+DMR)     ", synctest_pos + 1, modulation);
	                      }
	                    lastsynctype = -1;
	                    return (12);
	                  }
	              }
	          }

	        if (exitflag == 1)
	          {
	            //cleanupAndExit (opts, state);
	          }

	        if (synctest_pos < 10200)
	          {
	            synctest_pos++;
	            synctest_p_counter++;
	          }
	        else
	          {
	            // buffer reset
	            synctest_pos = 0;
	            synctest_p_counter=0;
	            synctest_p = synctest_buf;
	            noCarrier (opts, state);
	          }

	        if (carrier == 1)
	          {
	            if (synctest_pos >= 1800)
	              {
	                if (errorbars == 1)
	                  {
	                    if (verbose > 1)
	                      {
	                        //printf ("Sync: no sync\n");
	                      }
	                  }
	                noCarrier (opts, state);
	                return (-1);
	              }
	          }
	      }

	    return (-1);
	  }
	  

}
