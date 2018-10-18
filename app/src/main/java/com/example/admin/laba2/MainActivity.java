package com.example.admin.laba2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    //объявление объектов

    //кнопка
    Button TempGet;

    //поля, отображающие символы
    TextView TempShow;
    TextView TempShowCPU;
    TextView TraffUpShow;
    TextView TraffDwShow;

    //переменная для хранения температуры батареи
    float BatteryTemp;

    //полля для ввода символов
    EditText MaxTemp;
    EditText MaxTraff;
    EditText ToMail;
    IntentFilter intentfilter;

    //Код, который будет выполнен после запуска приложения
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //инстализация и привязка интерфейса к переменным
        TempGet = (Button)findViewById(R.id.button1);
        TempShow = (TextView)findViewById(R.id.textView1);
        TempShowCPU = (TextView)findViewById(R.id.textView);
        TraffUpShow = (TextView)findViewById(R.id.textView4);
        TraffDwShow = (TextView)findViewById(R.id.textView3);
        intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        MaxTemp = (EditText) findViewById(R.id.num1);
        MaxTraff = (EditText) findViewById(R.id.traffMax);
        ToMail = (EditText) findViewById(R.id.editMail);

        //переменные для храниния значений загруженного трафика с момента включения устройства, до запуска приложения
        Gl.traffUp = TrafficStats.getMobileTxBytes();
        Gl.traffDw = TrafficStats.getMobileRxBytes();
        TempGet.setOnClickListener(new View.OnClickListener() {

            //Код, который будет выполнен после нажатия на кнопку
            @Override
            public void onClick(View v) {
                Gl.ifNot = false;

                //проверка заполненны ли поля для максимальной температуры и трафика
                if (MaxTemp.getText().length()!=0)
                    Gl.maxTm = Integer.parseInt(MaxTemp.getText().toString());
                if (MaxTraff.getText().length()!=0)
                    Gl.maxTraff = Long.parseLong(MaxTraff.getText().toString());

                //скрытие ненужных элементов интерцейса
                MaxTemp.setVisibility(EditText.GONE);
                ToMail.setVisibility(EditText.GONE);
                MaxTraff.setVisibility(EditText.GONE);
                TempGet.setVisibility(Button.GONE);
                //Поток, в котором будет выполняться программа
                // TODO Auto-generated method stub
                Runnable runnable = new Runnable(){
                    @Override
                    public void run(){
                        while (!Thread.currentThread().isInterrupted()){
                            try {
                                Thread.sleep(1000);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.this.registerReceiver(broadcastreceiver,intentfilter);
                                    }
                                });
                            } catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    }
                };
                Thread t=new Thread(runnable);
                t.start();
            }
        });

    }
    //Класс для хранения переменных
    public static class Gl{
        public static long traffDw,traffUp,traffDwNow,traffUpNow,maxTraff;
        public static float maxTm;
        public static boolean ifNot;

    }
    //Метод, получающий температуру процессора
    public float getCpuTemp() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine();
            float temp = Float.parseFloat(line) / 1000.0f;

            return temp;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    //Метод для отправки письма. На вход получает строку с темой письма и содержимым письма
    private void sendEmail(String subject, String message) {
        String email;
        if (ToMail.getText().length()!=0){
             email = ToMail.getText().toString().trim();
             SendMail sm = new SendMail(this, email, subject, message);
             sm.execute();
            }
    }

    //Метод выводящий и обновляющий значения в полях на экране
    public BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //получение трафика с момента запуска
            Gl.traffDwNow = Math.abs(Gl.traffDw-TrafficStats.getMobileRxBytes());
            Gl.traffUpNow = Math.abs(Gl.traffUp-TrafficStats.getMobileTxBytes());

            //получение температуры батареи
            BatteryTemp = (float)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;

            //вывод значений в поля на экране
            TempShow.setText("Батарея: " + BatteryTemp + " " + (char) 0x00B0 + "C");
            TempShowCPU.setText("CPU: " + getCpuTemp() + " " + (char) 0x00B0 + "C");
            TraffDwShow.setText("Загруженно: "+ String.valueOf(Gl.traffDwNow/1048576)+" MБ "+String.valueOf((Gl.traffDwNow%1048576)/1024)+" КБ " + String.valueOf(Gl.traffDwNow%1024)+" Б");
            TraffUpShow.setText("Переданно: " + String.valueOf(Gl.traffUpNow/1048576)+" MБ "+String.valueOf((Gl.traffUpNow%1048576)/1024)+" КБ " + String.valueOf(Gl.traffUpNow%1024)+" Б");

            //Выполняется в случае, если письмо еще не было отправлено
           if (Gl.ifNot==false){

               //Выполняется в случае, если были заполненны оба поля
               if (MaxTemp.getText().length()!=0&&MaxTraff.getText().length()!=0) {
                   if (((Gl.maxTm < Math.round(getCpuTemp())) || (Gl.maxTm < Math.round(BatteryTemp)))&&(Gl.traffDwNow/1048576 > Gl.maxTraff)) {
                       sendEmail("Превышены трафик и температура","Температура процессора составляет: "+getCpuTemp()+"\n Температура батареи составляет: "+BatteryTemp+"\n Загруженно через мобильный интернет: "+ String.valueOf(Gl.traffDwNow/1048576)+" MБ "+String.valueOf((Gl.traffDwNow%1048576)/1024)+" КБ " + String.valueOf(Gl.traffDwNow%1024)+" Б");
                       Gl.ifNot = true;
                   }
               }

               //Если была указа только критическая температура
               else if (MaxTemp.getText().length()!=0&&MaxTraff.getText().length()==0){
                   if (Gl.maxTm < Math.round(getCpuTemp()) || Gl.maxTm < Math.round(BatteryTemp))
                       sendEmail("Превышена температура","Температура процессора составляет: "+getCpuTemp()+"\n Температура батареи составляет: "+BatteryTemp);
                        Gl.ifNot = true;
               }

               //Если был указан только максимальный трафик
               else if (MaxTemp.getText().length()==0&&MaxTraff.getText().length()!=0){
                   if ((Gl.traffDwNow/1048576 > Gl.maxTraff)) {
                       sendEmail("Превышен трафик","Загруженно через мобильный интернет: "+ String.valueOf(Gl.traffDwNow/1048576)+" MБ "+String.valueOf((Gl.traffDwNow%1048576)/1024)+" КБ " + String.valueOf(Gl.traffDwNow%1024)+" Б");
                       Gl.ifNot = true;
                   }
               }
            }
        }
    };
}