package ioio.examples.hello;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        		String s="";
        		switch (msg.what){
        			case 0x101:
        				s= "num:"+msg.obj.toString()+"\n";
        				break;
        			case 0x102:
        				s= "received:"+msg.obj.toString()+"\n";
        		}
				MainActivity.this.textview_.append(s);
				
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
			uart = ioio_.openUart(37, 38, 38400, Uart.Parity.NONE,Uart.StopBits.ONE );
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
			
			if(reading==false)
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
				Message msg =new Message();
				msg.what=0x101;
				msg.obj=availableBytes;
				MainActivity.mHandler.sendMessage(msg);
				if(availableBytes>0){
				byte[] rbuf=new byte[100];
				
					in.read(rbuf,0,availableBytes);
					char[] temp = (new String(rbuf,0,availableBytes)).toCharArray();
					String temp2=new String(temp);
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
}