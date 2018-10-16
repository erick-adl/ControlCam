package info.androidhive.securitycam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import info.androidhive.securitycam.MqttService.LocalBinder;


import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private TextView txStatus;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private static String selectedFromList;


    boolean mBounded;
    MqttService mBoundService;


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


    @Override
    protected void onStart() {
        super.onStart();

        Intent mIntent = new Intent(this, MqttService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    };

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mBounded = false;
            mBoundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            LocalBinder mLocalBinder = (LocalBinder)service;
            mBoundService = mLocalBinder.getService();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };


    public void updateListBoards(String status) {
        if (!arrayList.contains(status)) {
            arrayList.add(status);
            adapter.notifyDataSetChanged();
        }
    }

    public void changeStatusConectionView(String status, int color) {
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


        findViewById(R.id.btEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSelectedBoard() != null) {

                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                    View mView = getLayoutInflater().inflate(R.layout.dialog_edit, null);
                    final EditText etNomeEquipamento = (EditText) mView.findViewById(R.id.etNomeEquipamento);
                    etNomeEquipamento.setHint("Editar " + getSelectedBoard());
                    Button mLogin = (Button) mView.findViewById(R.id.btSalvar);
                    mBuilder.setView(mView);
                    final AlertDialog dialog = mBuilder.create();
                    dialog.show();
                    mLogin.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!etNomeEquipamento.getText().toString().isEmpty()) {

                                mBoundService.publish("ControlCamProject/setname/" + Delagate.mainActivity.getSelectedBoard(), etNomeEquipamento.getText().toString());

                                Toast.makeText(MainActivity.this, "Equipamento será alterado em instantes", Toast.LENGTH_SHORT).show();
                                arrayList.clear();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Nome de equipamento não pode ser em branco",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });


    }


    public String getSelectedBoard() {
        if (list.getCheckedItemPosition() < 0) {
            return null;
        }
        return adapter.getItem(list.getCheckedItemPosition());
    }

    private void initMqttService() {
        startService(new Intent(MainActivity.this, MqttService.class));
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
//                Toast.makeText(this,
//                        "Draw over other app permission not available. Closing the application",
//                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}



