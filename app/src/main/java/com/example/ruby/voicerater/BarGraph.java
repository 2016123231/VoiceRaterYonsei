package com.example.ruby.voicerater;

import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;


public class BarGraph extends Fragment {

    Bundle params;
    int length;
    ProgressBar[] progressBars;

    public BarGraph(){
        params = new Bundle(0);
        length = 0;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        params = args;
        length = args.size();
        progressBars = new ProgressBar[length];
    }

    private void setGraph(FrameLayout layout){
        TableLayout table = layout.findViewById(R.id.resultTable);
        table.setWeightSum(length);

        for(int i=0;i<length;i++){
            View view = getLayoutInflater().inflate(R.layout.bar_graph_layout, table,false);
            TextView name = view.findViewById(R.id.tagName);
            ProgressBar valueGraph = view.findViewById(R.id.valueGraph);
            progressBars[i] = valueGraph;
            TextView valueNum = view.findViewById(R.id.valueNum);

            String curKey = "p" + (i+1);
            name.setText(getResources().getString(getResources().getIdentifier("param_"+(i+1),"string","com.example.ruby.voicerater")));
            double curValue = params.getDouble(curKey);
            int curValueToInt = (int) ( curValue * 1000 );
            if(Build.VERSION.SDK_INT >= 24){
                valueGraph.setProgress(curValueToInt, true);
            }else{
                valueGraph.setProgress(curValueToInt);
            }
            String toDisplay = curValue + "/6.0";
            valueNum.setText(toDisplay);

            table.addView(view);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.fragment_bar_graph, container, false);
        setGraph(layout);

        return layout;
    }
}
