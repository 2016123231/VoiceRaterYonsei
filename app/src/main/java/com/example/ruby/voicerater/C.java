package com.example.ruby.voicerater;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class C extends AppCompatActivity {

    Bundle result;//bundle sent from activity Ba
    BarGraph barGraph;//bar-graph-resultscreen

    public void setResultScreen(){
        //get result from server <server>
        result = getIntent().getBundleExtra("results");

        //set view pager
        final ViewPager results = findViewById(R.id.results);
        results.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch(position)
                {
                    case 0:
                        barGraph = new BarGraph();
                        barGraph.setArguments(result);
                        return barGraph;
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 1;
//                return 2;
            }
        });
        results.setCurrentItem(0);
        View.OnClickListener movePageListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int tag = (int) view.getTag();
                results.setCurrentItem(tag);
            }
        };

    }

    //alert before closure
    public void alert(){
        AlertDialog.Builder ask = new AlertDialog.Builder(this);
        ask.setMessage("Are you sure to exit?");
        ask.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //finish activity
                finish();
            }
        });
        ask.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        ask.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c);

        Button exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert();
            }
        });

        //set result screen
//        setResultScreen();
    }

    //remove activity when its off screen
    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    //on back key pressed, ask once
    @Override
    public void onBackPressed(){
        alert();
    }
}
