package com.example.mymeds;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mymeds.classes.Database;
import com.example.mymeds.dto.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Database database = null;
    public static final String defaultNotifChannel = "Default";
    public static final String defaultNotifChannelDesc = "Default channel for medicine reminders";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);
        setStatusBarHeight();
        database = new Database(getApplicationContext(), "MyMeds");
        createNotifChannel();
        getReminders();
    }

    private void createNotifChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(defaultNotifChannel, defaultNotifChannelDesc, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setStatusBarHeight()
    {
        LinearLayout layout =  findViewById(R.id.main_ll);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)layout.getLayoutParams();
        params.setMargins(0, getStatusBarHeight(), 0, 0);
        layout.setLayoutParams(params);
    }

    private int getStatusBarHeight()
    {
        int result = 24;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;

    }
    private void showToast(String message)
    {
         Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    public void saveMedicine(View view)
    {
        EditText nameInput = findViewById(R.id.med_name);
        TextView dateInput = findViewById(R.id.med_date);
        TextView timeText = findViewById(R.id.med_time);

        String medName = nameInput.getText().toString().trim();
        String medDate = dateInput.getText().toString().trim();
        String medTime = timeText.getText().toString().trim();

        if(medName.isEmpty() || medDate.isEmpty() || medTime.isEmpty())
        {
            showMessage("Please enter all details");
            return;
        }

        insertReminder(medName, medDate, medTime);
        nameInput.setText("");
        dateInput.setText("");
        timeText.setText("");
        setAlarm(medName, medDate, getActualTime(medTime));
    }

    private void setAlarm(String medName, String medDate, String medTime)
    {
        String day = medDate.split("/")[0];
        String month =  medDate.split("/")[1];
        String year =  medDate.split("/")[2];

        String hourOfDay = medTime.split(":")[0];
        String minOfDay =  medTime.split(":")[1];
        String secOfDay =  medTime.split(":")[2];

        Context context = getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
        calendar.set(Calendar.MONTH, Integer.parseInt(month));
        calendar.set(Calendar.YEAR, Integer.parseInt(year));

        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourOfDay));
        calendar.set(Calendar.MINUTE, Integer.parseInt(minOfDay));
        calendar.set(Calendar.SECOND, Integer.parseInt(secOfDay));
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("med_name", medName);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, (int)System.currentTimeMillis(), intent, 0);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);

    }

    private void showMessage(String message)
    {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",null)
                .show();
    }


    public void getReminders()
    {
        SQLiteDatabase db = database.getReadableDatabase();

        String[] colsRequired = {"id","name","date","time"};

        List<Reminder> remList = new ArrayList<>();

        //select id, name, date, time from Reminder;
        try (Cursor cursor = db.query("Reminder", colsRequired, null, null, null, null, null))
        {
            while(cursor.moveToNext())
            {
                long id = cursor.getLong(cursor.getColumnIndex("id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                String time = cursor.getString(cursor.getColumnIndex("time"));

                Reminder reminder = new Reminder();
                reminder.setId(id);
                reminder.setName(name);
                reminder.setDate(date);
                reminder.setTime(time);

                remList.add(reminder);
            }
        }


        TextView resultView = findViewById(R.id.results);
        StringBuilder res = new StringBuilder();
        for(Reminder reminder : remList)
        {
            res.append(reminder.getName() + " " + reminder.getDate() + " " + reminder.getTime() + "\n");
        }
        if(!remList.isEmpty())
            resultView.setText(res.toString());
        else resultView.setText("No reminders");
    }

    public void clearReminders(View view)
    {
        SQLiteDatabase db = database.getWritableDatabase();
        db.execSQL("DELETE FROM Reminder");
        showToast("All reminders cleared");
        getReminders();
    }
    private void insertReminder(String name, String date, String timeOfDay)
    {
        String time = getActualTime(timeOfDay);

        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("date", date);
        contentValues.put("time", time);

        SQLiteDatabase db = database.getWritableDatabase();

        long id = db.insert("Reminder", null, contentValues);
        if(id == -1)
        {
            showToast("Something went wrong, reminder was not saved");
        }
        else
        {
            showToast("Reminder Saved");
            getReminders();
        }
    }

    private String getActualTime(String timeOfDay)
    {
        switch (timeOfDay)
        {
            case "Morning" : return "8:00:00";
            case "Afternoon" : return "12:00:00";
            case "Evening" : return "16:00:00";
            case "Night" : return "20:00:00";
            case "Immediately" : return "00:00:00";
        }
        return null;
    }

    public void dateDialog(View view)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DatePickerDialog dialog = new DatePickerDialog(this);

            dialog.setOnDateSetListener((datePicker, year, month, day) -> {
               TextView dateView = findViewById(R.id.med_date);
               dateView.setText(day + "/" + month + "/" + year);
            });
            dialog.show();
        }
        else
        {
            showToast("DatePicker requires Android Nougat and above. Get a new phone!");
        }
    }

    public void todDialog(View view)
    {
        String[] todOptions = {"Morning", "Afternoon","Evening","Night", "Immediately"};
        new AlertDialog.Builder(this)
                .setTitle("Time of day")
                .setCancelable(true)
                .setItems(todOptions, (dialogInterface, i) -> {
                    TextView todView = findViewById(R.id.med_time);
                    todView.setText(todOptions[i]);
                }).show();
    }
}