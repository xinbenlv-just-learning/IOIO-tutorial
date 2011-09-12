package ioio.examples.hello;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import ioio.examples.hello.R;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link AbstractIOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends AbstractIOIOActivity {
	private ToggleButton button_;
	private TextView textview_;
	public static Handler mHandler;
	public static int DISPLAY_SIZE=30;
	public LinkedList<String> tvbuf=new LinkedList<String>();
	public Integer i=0;
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textview_ = (TextView) findViewById(R.id.title);
		
        mHandler=new Handler(){
        	public void handleMessage(Message msg)
        	{
        		i++;
        		String s="";
        		switch (msg.what){
        			case 0x101:
        				s= "num:"+msg.obj.toString();
        				break;
        			case 0x102:
        				s= "received:"+msg.obj.toString();
        		}
				tvbuf.addLast(i.toString()+"|"+s);
				if(tvbuf.size()>DISPLAY_SIZE)tvbuf.removeFirst();
				MainActivity.this.textview_.setText("");
        		for (String str : tvbuf){
    				MainActivity.this.textview_.append(str+"\n");
        		}
				
        		super.handleMessage(msg);
        	}
        };
		
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		/** The on-board LED. */
		private DigitalOutput led_;
		private Uart uart;
		private InputStream in;
		private OutputStream out;
		private byte[] wbuf={'h','e','l','l','o'};
		private boolean reading=false;//false
		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);
			uart = ioio_.openUart(37, 38, 115200, Uart.Parity.NONE,Uart.StopBits.ONE );
			in = uart.getInputStream();
			out = uart.getOutputStream();
			
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws IOException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		protected void loop() throws ConnectionLostException {
			boolean always_reading=false;//Always reading
			if(reading==false && always_reading==false)
			{
				
				try {
					out.write(wbuf);
					out.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Message msg1 =new Message();
					msg1.what=0x102;
					msg1.obj=e.getMessage();
					MainActivity.mHandler.sendMessage(msg1);
					reading =false;
				}
				reading=true;
				led_.write(true);
			}
			else{
				try {
				
				int availableBytes =in.available(); 
				if(always_reading ==true &&availableBytes==0){return;}
				Message msg =new Message();
				msg.what=0x101;
				msg.obj=availableBytes;
				MainActivity.mHandler.sendMessage(msg);
				if(availableBytes>0){
				int readBytes=Math.min(availableBytes, 41);
					byte[] rbuf=new byte[readBytes];
					
					in.read(rbuf,0,readBytes);
					//byte[] temp = (new String(rbuf,0,readBytes)).toCharArray();
					String temp2= MainActivity.byte2HexString(rbuf);
					
					Message msg1 =new Message();
					msg1.what=0x102;
					msg1.obj=temp2;
					MainActivity.mHandler.sendMessage(msg1);
				}
		
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					
					Message msg1 =new Message();
					msg1.what=0x102;
					msg1.obj=e.getMessage();
					MainActivity.mHandler.sendMessage(msg1);
					reading =false;
				}
				reading=false;
				led_.write(false);	
			}
			
			try {
				sleep(300);
			} 
				catch (InterruptedException e) {
					Message msg1 =new Message();
					msg1.what=0x102;
					msg1.obj=e.getMessage();
					MainActivity.mHandler.sendMessage(msg1);
					reading =false;
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
	
    public static String byte2HexString(byte[] b) {
        char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7',
                      '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] newChar = new char[b.length * 3];
        for(int i = 0; i < b.length; i++) {
            newChar[3 * i] = hex[(b[i] & 0xf0) >> 4];
            newChar[3 * i + 1] = hex[b[i] & 0xf];
            newChar[3 * i + 2] = ' ';
        }
        return new String(newChar);
    }
    
    public static int calcByte(int crc, int b) {
        crc = crc ^ (int)b << 8;

        for (int i = 0; i < 8; i++) {
  	if ((crc & 0x8000) == 0x8000)
  	  crc = crc << 1 ^ 0x1021;
  	else
  	  crc = crc << 1;
        }

        return crc & 0xffff;
      }
    public static int calc(byte[] packet, int index, int count) {
    	int crc = 0;
    	
    	while (count > 0) {
    	    crc = calcByte(crc, packet[index++]);
    	    count--;
    	}
    	return crc;
        }

        public static int calc(byte[] packet, int count) {
    	return calc(packet, 0, count);
        }

        public static void set(byte[] packet) {
            int crc = calc(packet, packet.length - 2);

            packet[packet.length - 2] = (byte) (crc & 0xFF);
            packet[packet.length - 1] = (byte) ((crc >> 8) & 0xFF);
        }
}

//7E 45 00 FF FF 00 00 1C 00 64 61 62 63 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F 70 71 72 73 74 75 76 77 78 2C E9 7E
