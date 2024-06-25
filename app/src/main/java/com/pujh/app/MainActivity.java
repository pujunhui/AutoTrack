package com.pujh.app;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.pujh.app.databinding.ActivityMainBinding;
import com.pujh.autotrack.SensorsDataAPI;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;


@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity {
    private final static int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainBinding.setHandlers(this);

        setTitle("Home");

        ButterKnife.bind(this);

        initTextView();
        initImageView();
        initButton();
        initLambdaButton();
        initShowDialogButton();
        initShowMultiChoiceDialogButton();
        initCheckBox();
        initRadioButton();
        initRadioGroup();
        initToggleButton();
        initSwitchCompat();
        initRatingBar();
        initSeekBar();
        initSpinner();
        initAdapterViewTest();
        initExpandableListViewTest();
        initTabHostButton();
//
//        findViewById(R.id.btn_GoProcess).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, ProcessActivity.class);
//                MainActivity.this.startActivity(intent);
//            }
//        });
    }

    private void initRadioGroup() {
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

            }
        });
    }

    private void initSwitchCompat() {
        SwitchCompat switchCompat = findViewById(R.id.switchCompat);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
    }

    private void initToggleButton() {
        ToggleButton toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
    }

    private void initRadioButton() {
        RadioButton radioButton = findViewById(R.id.radioButton);
        radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
    }

    private void initImageView() {
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initTextView() {
        TextView textView = findViewById(R.id.textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initTabHostButton() {
        Button button = findViewById(R.id.tabHostButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TabHostTestActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initExpandableListViewTest() {
        Button button = findViewById(R.id.expandableListViewTest);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ExpandableListViewTestActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initAdapterViewTest() {
        Button button = findViewById(R.id.adapterViewTest);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AdapterViewTestActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initSpinner() {
        Spinner spinner = findViewById(R.id.spinner);
        List<String> dataList = new ArrayList<>();
        dataList.add("北京");
        dataList.add("上海");
        dataList.add("广州");
        dataList.add("深圳");
        dataList.add("咸宁");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dataList);

        //为适配器设置下拉列表下拉时的菜单样式。
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //为spinner绑定我们定义好的数据适配器
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {


            }
        });
    }

    private void initSeekBar() {
        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initRatingBar() {
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

            }
        });
    }

    private void initCheckBox() {
        CheckBox checkBox = findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
    }

    private void initShowMultiChoiceDialogButton() {
        Button button = findViewById(R.id.showMultiChoiceDialogButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMultiChoiceDialog(MainActivity.this);
            }
        });
    }


    private void initShowDialogButton() {
        Button button = findViewById(R.id.showDialogButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(MainActivity.this);
            }
        });
    }

    /**
     * Lambda 语法
     */
    private void initLambdaButton() {
        Button button = findViewById(R.id.lambdaButton);
        button.setOnClickListener(view -> showToast("Lambda OnClick"));
    }

    /**
     * 普通 setOnClickListener
     */
    private void initButton() {
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("普通");
            }
        });

        registerForContextMenu(button);
    }

    /**
     * 通过 DataBinding 绑定点击事件
     *
     * @param view View
     */
    public void dataBindingOnClick(View view) {
        showToast("DataBinding Onclick");
    }

    /**
     * 通过 layout 中的 Android:onClick 属性绑定点击事件
     *
     * @param view View
     */
    public void xmlOnClick(View view) {
        showToast("XML OnClick");
    }

    /**
     * 通过 ButterKnife 绑定事件
     */
    @OnClick({R2.id.butterknife})
    public void butterKnifeButtonOnClick(View view) {
        showToast("Butter Knife OnClick");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showMultiChoiceDialog(Context context) {
        Dialog dialog;
        boolean[] selected = new boolean[]{true, true, true, true};
        CharSequence[] items = {"北京", "上海", "深圳", "合肥"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("神策数据在哪些城市有服务团队？");
        DialogInterface.OnMultiChoiceClickListener mutiListener =
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which, boolean isChecked) {
                        selected[which] = isChecked;
                    }
                };
        builder.setMultiChoiceItems(items, selected, mutiListener);
        dialog = builder.create();
        SensorsDataAPI.getInstance().trackDialog(this, dialog);
        dialog.show();
    }

    private void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("标题");
        builder.setMessage("内容");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.create();
        SensorsDataAPI.getInstance().trackDialog(this, dialog);

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        showToast(item.getTitle().toString());
        return super.onContextItemSelected(item);
    }

    @Override
    @SuppressWarnings("all")
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_more) {
            ViewGroup rootView = findViewById(R.id.rootView);
            Button button = new Button(this);
            button.setText("动态创建的 Button");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            rootView.addView(button);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            //拥有权限
        } else {
            //没有权限，需要申请全新啊
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SensorsDataAPI.getInstance().ignoreAutoTrackActivity(MainActivity.class);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 用户点击允许
                } else {
                    // 用户点击禁止
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SensorsDataAPI.getInstance().removeIgnoredActivity(MainActivity.class);
    }
}
