package by.stanislau.accelerometerapp;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private final static String TAG = "Сенсоры";
	private final static String TAG1 = "Значение";
	final String DIR_SD = "AccelerationData";
	final String FILENAME_SD = "data.txt";
	private final static String fileNameProection = "Proections.xls";
	StringBuilder sb = new StringBuilder();
	SensorManager sensorManager;
	Sensor sensorAccel;
	Timer timer;
	boolean startStop = false;
	boolean calibr = false;
	TextView showData;
	ArrayList<Double> arrayAlpha_x;
	ArrayList<Double> arrayAlpha_y;
	ArrayList<Double> arrayAlpha_z;
	ArrayList<Double> temp_arrayAlpha_x = new ArrayList<Double>();
	ArrayList<Double> temp_arrayAlpha_y = new ArrayList<Double>();
	ArrayList<Double> temp_arrayAlpha_z = new ArrayList<Double>();

	int globalI;
	DecimalFormat myFormat;
	File sdPath;
	Switch calibration;

	public double sr_x = 0.0;
	public double sr_y = 0.0;
	public double sr_z = 0.0;
	
	public double distancia_x = 0.0;
	public double distancia_y = 0.0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button startWrite = (Button) findViewById(R.id.start_move);
		startWrite.setOnClickListener(this);
		Button stopWrite = (Button) findViewById(R.id.stop_move);
		stopWrite.setOnClickListener(this);
		calibration = (Switch) findViewById(R.id.switch1);
		calibration.setOnCheckedChangeListener(onoffListener);
		showData = (TextView) findViewById(R.id.showData);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		arrayAlpha_x = new ArrayList<Double>();
		arrayAlpha_y = new ArrayList<Double>();
		arrayAlpha_z = new ArrayList<Double>();
		temp_arrayAlpha_x = new ArrayList<Double>();
		temp_arrayAlpha_y = new ArrayList<Double>();
		temp_arrayAlpha_z = new ArrayList<Double>();
		myFormat = new DecimalFormat("###.##");

		// проверяем доступность SD
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Log.d(TAG,
					"SD-карта не доступна: "
							+ Environment.getExternalStorageState());
			return;
		}
		// получаем путь к SD
		sdPath = Environment.getExternalStorageDirectory();
		Log.d(TAG, "Получили путь");
		// добавляем свой каталог к пути
		sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
		Log.d(TAG, "Добавили свой каталог");
		// создаем каталог
		sdPath.mkdirs();

	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(listener, sensorAccel,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.d(TAG, "Зарегистрировали sensorAccel");
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(listener);
	}

	OnCheckedChangeListener onoffListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			Log.d("Switch State=", "" + isChecked);

			calibr = isChecked;
			if(calibr){
				temp_arrayAlpha_x = new ArrayList<Double>();
				temp_arrayAlpha_y = new ArrayList<Double>();
				temp_arrayAlpha_z = new ArrayList<Double>();
			} else if (!calibr){
				CalculateSrValue(temp_arrayAlpha_x, temp_arrayAlpha_y,
						temp_arrayAlpha_z);
				distancia_x = 0.0;
			}

		}
	};

	SensorEventListener listener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {

			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:

				double tempX = ((double) Math.round(event.values[0] * 100)) / 100;
				double tempY = ((double) Math.round(event.values[1] * 100)) / 100;
				double tempZ = ((double) Math.round(event.values[2] * 100)) / 100;

				if (calibr) {
					temp_arrayAlpha_x.add(tempX);
					temp_arrayAlpha_y.add(tempY);
					temp_arrayAlpha_z.add(tempZ);
				}

				if (startStop) {

					Log.d(TAG1, "Есть значение!");
					showData.setText("x - " + Double.toString(tempX) + "\ny - "
							+ Double.toString(tempY) + "\nz - "
							+ Double.toString(tempZ));



						arrayAlpha_x.add(globalI, tempX - sr_x);
						arrayAlpha_y.add(globalI, tempY - sr_y);
						arrayAlpha_z.add(globalI, tempZ - sr_z);
						
						globalI++;
					

				} else {
					showData.setText("Stoped");
				}
				break;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.start_move:
			Toast.makeText(this, "Поехали...", Toast.LENGTH_SHORT).show();
			globalI = 0;
			startStop = true;
			break;
		case R.id.stop_move:
			Toast.makeText(this, "Стоооп...", Toast.LENGTH_SHORT).show();
			Toast.makeText(this,
					"Всего " + Integer.toString(globalI) + " показаний",
					Toast.LENGTH_SHORT).show();
			startStop = false;
			writeProectionXlSFile();
			break;
		}

	}

	void writeProectionXlSFile() {
		// create file in directory
		File file = new File(sdPath, fileNameProection);
		WorkbookSettings wbSettings = new WorkbookSettings();
		wbSettings.setLocale(new Locale("en", "EN"));
		WritableWorkbook workbook;
		try {
			workbook = Workbook.createWorkbook(file, wbSettings);
			WritableSheet sheet = workbook.createSheet("Лист 1", 0);
			Label labe00 = new Label(0, 0, "a");
			Label labe10 = new Label(1, 0, "alpha_x");
			Label labe20 = new Label(2, 0, "alpha_y");
			Label labe30 = new Label(3, 0, "alpha_z");
			Label labe60 = new Label(6, 0, "v_x");
			Label labe70 = new Label(7, 0, "v_y");
			Label labe80 = new Label(8, 0, "v_z");
			try {
				sheet.addCell(labe00);
				sheet.addCell(labe10);
				sheet.addCell(labe20);
				sheet.addCell(labe30);
				sheet.addCell(labe60);
				sheet.addCell(labe70);
				sheet.addCell(labe80);
				int y0 = 1;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(0, y0, Double.toString(
							VectorAbs(arrayAlpha_x.get(i), arrayAlpha_y.get(i),
									arrayAlpha_z.get(i))).replace('.', ',')));
					y0++;
				}
				int y1 = 1;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(1, y1, Double.toString(
							arrayAlpha_x.get(i)).replace('.', ',')));
							distancia_x = distancia_x + (arrayAlpha_x.get(i)*0.36*0.36);
					y1++;
				}
				int y2 = 1;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(2, y2, Double.toString(
							arrayAlpha_y.get(i)).replace('.', ',')));
							distancia_y = distancia_y + (arrayAlpha_y.get(i)*0.36*0.36);
					y2++;
				}
				int y3 = 1;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(3, y3, Double.toString(
							arrayAlpha_z.get(i)).replace('.', ',')));
					y3++;
				}
				int y4 = 1;
				double v_x = 0.0;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(6, y4, Double.toString(v_x
							+ arrayAlpha_x.get(i) * 0.6)));
					v_x = v_x + arrayAlpha_x.get(i) * 0.06;
					y4++;
				}
				int y5 = 1;
				double v_y = 0.0;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(7, y5, Double.toString(v_y
							+ arrayAlpha_y.get(i) * 0.6)));
					v_y = v_y + arrayAlpha_y.get(i) * 0.06;
					y5++;
				}
				int y6 = 1;
				double v_z = 0.0;
				for (int i = 0; i < globalI; i++) {
					sheet.addCell(new Label(8, y6, Double.toString(v_z
							+ arrayAlpha_z.get(i) * 0.6)));
					v_z = v_z + arrayAlpha_z.get(i) * 0.06;
					y6++;
				}
			} catch (RowsExceededException e) {
				e.printStackTrace();
			} catch (WriteException e) {
				e.printStackTrace();
			}

			workbook.write();
			try {
				workbook.close();
			} catch (WriteException e) {
				e.printStackTrace();
			} // createExel(exelsheet)

		} catch (Exception e) {
			// TODO: handle exception
		}
		Toast.makeText(getApplicationContext(), "Запись закончена",
				Toast.LENGTH_SHORT).show();
		Log.d("Дистанция", Double.toString(distancia_x));
		Log.d("Дистанция", Double.toString(distancia_y));
	}

	String format(float values[]) {
		String.format(Locale.UK, "EN");
		return String.format("%1$.1f\t\t%2$.1f\t\t%3$.1f", values[0],
				values[1], values[2]);
	}

	public static double Solution(double side1, double side2) {

		if (side1 == 0) {
			side1 = side1 + 0.0001;
		}
		if (side2 == 0) {
			side2 = side2 + 0.0001;
		}

		double rez = side1 / side2;

		return rez;
	}

	public static double VectorAbs(double pX, double pY, double pZ) {
		double vectorAbs = ((double) Math.round(Math.pow((Math.pow(pX, 2)
				+ Math.pow(pY, 2) + Math.pow(pZ, 2)), 0.5) * 100)) / 100;
		return vectorAbs;
	}

	public void CalculateSrValue(ArrayList<Double> ar_x,
			ArrayList<Double> ar_y, ArrayList<Double> ar_z) {
		for (int i = 0; i < ar_x.size(); i++) {
			sr_x = sr_x + ar_x.get(i);
		}
		sr_x = sr_x / ar_x.size();
		for (int i = 0; i < ar_y.size(); i++) {
			sr_y = sr_y + ar_y.get(i);
		}
		sr_y = sr_y / ar_y.size();
		for (int i = 0; i < ar_z.size(); i++) {
			sr_z = sr_z + ar_z.get(i);
		}
		sr_z = sr_z / ar_z.size();
		Log.d("Среднее по Х", Double.toString(sr_x));
		Log.d("Среднее по Y", Double.toString(sr_y));
		Log.d("Среднее по Z", Double.toString(sr_z));
	}

}
