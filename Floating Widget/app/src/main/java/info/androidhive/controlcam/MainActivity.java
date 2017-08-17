package info.androidhive.controlcam;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private TextView txStatus;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private static String selectedFromList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            initializeView();
            initMqttService();
            Delagate.mainActivity = this;
        }

        if (getIntent().getBooleanExtra("EXIT", false)) {
            stopService(new Intent(this, MqttService.class));
            finish();
            return;
        }
    }

    public void updateListBoards(String status){
        if(!arrayList.contains(status)){
            arrayList.add(status);
            adapter.notifyDataSetChanged();
        }
    }

    public void changeStatusConectionView(String status, int color){
        txStatus = (TextView) findViewById(R.id.tvStatusConnection);
        txStatus.setText(status);
        txStatus.setTextColor(color);
    }

    private void initializeView() {
        list = (ListView) findViewById(R.id.lvBoardsOnline);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, R.layout.custon_layout, arrayList);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setAdapter(adapter);



        findViewById(R.id.btStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSelectedBoard() != null) {
                    startService(new Intent(MainActivity.this, FloatingViewService.class));
                    finish();
                }
            }
        });


    }


    public String getSelectedBoard(){
        if(list.getCheckedItemPosition() < 0){
            return null;
        }
        return adapter.getItem(list.getCheckedItemPosition());
    }

    private void initMqttService() {
        startService(new Intent(MainActivity.this, MqttService.class));
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initializeView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}



