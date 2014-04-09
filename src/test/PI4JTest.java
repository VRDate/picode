package test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class PI4JTest {

	I2CBus bus;
	I2CDevice device;
	byte[] bytes;
	static int SENSITIVITY = 16384; // sensor sencitivity

	public static void main(String[] args) throws IOException {
		PI4JTest pit = new PI4JTest();
		pit.testStuff();
		pit.startReading();
	}

	public PI4JTest() throws IOException {
		System.out.println("Starting sensors reading:");
		bus = I2CFactory.getInstance(I2CBus.BUS_1);
		System.out.println("Connected to bus OK!");
		// get device itself
		device = bus.getDevice(0x68);
		System.out.println("Connected to device OK!");
	}

	public void testStuff() throws IOException {
		
		// start sensing, using config registries 6B and 6C
		
		device.write(0x6B, (byte) 0b00000000);
		device.write(0x6C, (byte) 0b00000000);
		System.out.println("Configuring Device OK!");

		// config gyro
		device.write(0x1B, (byte) 0b00011000);
		// config accel
		device.write(0x1C, (byte) 0b00000100);
		System.out.println("Configuring sensors OK!");

	}

	public void startReading() {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					readingSensors();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		new Thread(task).start();
	}

	private void readingSensors() throws IOException {
		
		int numElements = 15;
		
		bytes = new byte[numElements];
		DataInputStream gyroIn;
		
		while (true) {
			int r = device.read(0x3B, bytes, 0, bytes.length);

			if (r != numElements) { // 14 registries to be read, 6 for gyro, 6 for accel, and 2 for temp
				System.out.println("Error reading data, < " + bytes.length + " bytes");
			}
			gyroIn = new DataInputStream(new ByteArrayInputStream(bytes));
			
			short aVal = gyroIn.readShort();

			System.out.println("got data: " + aVal);
			

			try {
				Thread.sleep(700);
			} catch (InterruptedException ex) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
						null, ex);
			}
		}
	}

}