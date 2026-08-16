package com.questlog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    static class Quest { String id, title; int diff, xp; boolean done; long created; Quest(String id,String t,int d,boolean done,long c){this.id=id;this.title=t;this.diff=d;this.xp=d*10;this.done=done;this.created=c;} }

    List<Quest> quests = new ArrayList<>();
    String filter="all";
    int selectedDiff=5;

    RecyclerView recycler;
    QuestAdapter adapter;
    TextView rankBadge, heroLevel, rankLabel, xpLabel, diffLabel;
    ProgressBar xpBar;
    LinearLayout badgesRow;
    SeekBar diffSeek;
    EditText questInput;

    String[] rankIds={"E","D","C","B","A","S"};
    int[] rankMins={0,100,300,600,1000,1600};
    String[] rankNames={"E — Novice","D — Apprentice","C — Adept","B — Veteran","A — Master","S — Legend"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        rankBadge=findViewById(R.id.rankBadge);
        heroLevel=findViewById(R.id.heroLevel);
        rankLabel=findViewById(R.id.rankLabel);
        xpLabel=findViewById(R.id.xpLabel);
        xpBar=findViewById(R.id.xpBar);
        badgesRow=findViewById(R.id.badgesRow);
        diffSeek=findViewById(R.id.diffSeek);
        diffLabel=findViewById(R.id.diffLabel);
        questInput=findViewById(R.id.questInput);
        recycler=findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter=new QuestAdapter();
        recycler.setAdapter(adapter);

        load();
        diffSeek.setProgress(selectedDiff-1);
        updateDiffLabel();
        diffSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean fromUser){ selectedDiff=p+1; updateDiffLabel(); }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        findViewById(R.id.addBtn).setOnClickListener(v->addQuest());
        questInput.setOnEditorActionListener((v,actionId,event)->{ addQuest(); return true; });
        findViewById(R.id.tabAll).setOnClickListener(v->setFilter("all"));
        findViewById(R.id.tabActive).setOnClickListener(v->setFilter("active"));
        findViewById(R.id.tabDone).setOnClickListener(v->setFilter("done"));
        render();
    }

    void updateDiffLabel(){ diffLabel.setText("LV. "+selectedDiff+"  →  +"+(selectedDiff*10)+" XP"); }

    void addQuest(){
        String t=questInput.getText().toString().trim();
        if(t.isEmpty()){ Toast.makeText(this,"Enter a quest!",Toast.LENGTH_SHORT).show(); return; }
        quests.add(0,new Quest("q"+System.currentTimeMillis(),t,selectedDiff,false,System.currentTimeMillis()));
        questInput.setText(""); save(); render();
        Toast.makeText(this,"Quest added! LV."+selectedDiff+" +"+(selectedDiff*10)+" XP on clear",Toast.LENGTH_SHORT).show();
    }
    void setFilter(String f){ filter=f; render(); }
    int totalXp(){ int s=0; for(Quest q:quests) if(q.done) s+=q.xp; return s; }
    int rankIndex(int xp){ for(int i=rankMins.length-1;i>=0;i--) if(xp>=rankMins[i]) return i; return 0; }

    void render(){
        int xp=totalXp();
        int ri=rankIndex(xp);
        String rank=rankIds[ri];
        int nextMin = ri<rankMins.length-1 ? rankMins[ri+1] : rankMins[ri];
        boolean isMax = ri==rankMins.length-1;
        int range = isMax?1:(nextMin - rankMins[ri]);
        int prog = isMax?100: Math.round(((xp - rankMins[ri])/(float)range)*100);

        heroLevel.setText("LV."+(xp/100+1)+"  •  "+xp+" XP");
        rankLabel.setText(rankNames[ri]);
        xpLabel.setText(isMax ? xp+" / MAX" : xp+" / "+nextMin+" XP → "+rankIds[ri+1]);
        xpBar.setMax(100); xpBar.setProgress(prog);
        rankBadge.setText(rank);
        int[] rankColors={0xFF5a5a6a,0xFF2d8a4e,0xFF2e7bcf,0xFF7c3aed,0xFFe67e22,0xFFffd700};
        rankBadge.setBackgroundColor(rankColors[ri]);

        badgesRow.removeAllViews();
        for(int i=0;i<=ri;i++){
            TextView tv=new TextView(this);
            tv.setText(rankIds[i]); tv.setTextSize(10); tv.setTextColor(0xFF0e0e1a);
            tv.setPadding(14,6,14,6); tv.setBackgroundColor(rankColors[i]);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(0,0,8,0); tv.setLayoutParams(lp);
            badgesRow.addView(tv);
        }
        // highlight filter
        int activeBg=0xFFc9a227, idleBg=0xFF2a2440;
        findViewById(R.id.tabAll).setBackgroundColor(filter.equals("all")?activeBg:idleBg);
        findViewById(R.id.tabActive).setBackgroundColor(filter.equals("active")?activeBg:idleBg);
        findViewById(R.id.tabDone).setBackgroundColor(filter.equals("done")?activeBg:idleBg);

        adapter.notifyDataSetChanged();
    }

    void save(){
        try{
            JSONArray arr=new JSONArray();
            for(Quest q:quests){ JSONObject o=new JSONObject(); o.put("id",q.id); o.put("title",q.title); o.put("diff",q.diff); o.put("done",q.done); o.put("created",q.created); arr.put(o); }
            getSharedPreferences("questlog",MODE_PRIVATE).edit().putString("quests",arr.toString()).apply();
        }catch(Exception e){}
    }
    void load(){
        try{
            String raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests",null);
            if(raw!=null){ JSONArray arr=new JSONArray(raw); for(int i=0;i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); quests.add(new Quest(o.getString("id"),o.getString("title"),o.getInt("diff"),o.getBoolean("done"),o.optLong("created",System.currentTimeMillis()))); } return; }
        }catch(Exception e){}
        quests.add(new Quest("q1","Complete portfolio polish — Bloom & Branch",7,false,System.currentTimeMillis()));
        quests.add(new Quest("q2","Solve 3 DSA — arrays + DP",6,false,System.currentTimeMillis()-1000));
        quests.add(new Quest("q3","Read OS notes — paging",4,true,System.currentTimeMillis()-2000));
    }

    class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.VH>{
        List<Quest> filtered(){ List<Quest> l=new ArrayList<>(); for(Quest q:quests){ if(filter.equals("active")&&q.done) continue; if(filter.equals("done")&&!q.done) continue; l.add(q);} Collections.sort(l,(a,b)-> Boolean.compare(a.done,b.done)!=0 ? Boolean.compare(a.done,b.done) : Long.compare(b.created,a.created)); return l; }
        @Override public int getItemCount(){ return filtered().size(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p,int t){ return new VH(getLayoutInflater().inflate(R.layout.item_quest,p,false)); }
        @Override public void onBindViewHolder(VH h,int pos){
            Quest q=filtered().get(pos);
            h.title.setText(q.title);
            h.title.setAlpha(q.done?0.5f:1f);
            h.diffPill.setText("LV."+q.diff);
            h.xpPill.setText("+"+q.xp+" XP");
            int[] diffColors={0xFF2d8a4e,0xFF2d8a4e,0xFF2d8a4e,0xFFc9a227,0xFFc9a227,0xFFc9a227,0xFFe67e22,0xFFe67e22,0xFFff4444,0xFFff4444};
            h.diffPill.setBackgroundColor(diffColors[q.diff-1]);
            h.check.setChecked(q.done);
            h.check.setOnCheckedChangeListener(null);
            h.check.setOnCheckedChangeListener((v,checked)->{
                int before=rankIndex(totalXp());
                q.done=checked;
                int after=rankIndex(totalXp());
                save(); render();
                String msg=checked? "+"+q.xp+" XP!":"- "+q.xp+" XP";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if(checked && after>before){
                    Toast.makeText(MainActivity.this, "★ RANK UP! You are now "+rankIds[after]+" — "+rankNames[after], Toast.LENGTH_LONG).show();
                }
            });
            h.deleteBtn.setOnClickListener(v->{ quests.remove(q); save(); render(); });
        }
        class VH extends RecyclerView.ViewHolder{ TextView title,diffPill,xpPill,deleteBtn; CheckBox check; VH(android.view.View v){ super(v); title=v.findViewById(R.id.title); diffPill=v.findViewById(R.id.diffPill); xpPill=v.findViewById(R.id.xpPill); deleteBtn=v.findViewById(R.id.deleteBtn); check=v.findViewById(R.id.checkDone); } }
    }
}
