package com.questlog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
    String[] rankNames={"E - Novice","D - Apprentice","C - Adept","B - Veteran","A - Master","S - Legend"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        try {
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
            recycler.setNestedScrollingEnabled(false);
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
            View addBtn = findViewById(R.id.addBtn);
            if(addBtn!=null) addBtn.setOnClickListener(v->addQuest());
            questInput.setOnEditorActionListener((v,actionId,event)->{ addQuest(); return true; });
            View tabAll=findViewById(R.id.tabAll);
            View tabActive=findViewById(R.id.tabActive);
            View tabDone=findViewById(R.id.tabDone);
            if(tabAll!=null) tabAll.setOnClickListener(v->setFilter("all"));
            if(tabActive!=null) tabActive.setOnClickListener(v->setFilter("active"));
            if(tabDone!=null) tabDone.setOnClickListener(v->setFilter("done"));
            render();
        } catch(Exception e){
            android.util.Log.e("QuestLog", "onCreate crash", e);
            // Fallback minimal UI so app doesn't just die
            TextView tv=new TextView(this);
            tv.setText("Quest Log error: "+e.getMessage()+"\nPlease report this screen.");
            tv.setPadding(32,32,32,32);
            tv.setTextColor(0xFFFF4444);
            setContentView(tv);
        }
    }

    void updateDiffLabel(){ if(diffLabel!=null) diffLabel.setText("LV. "+selectedDiff+"  ->  +"+(selectedDiff*10)+" XP"); }

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
        try {
            int xp=totalXp();
            int ri=rankIndex(xp);
            String rank=rankIds[ri];
            int nextMin = ri<rankMins.length-1 ? rankMins[ri+1] : rankMins[ri];
            boolean isMax = ri==rankMins.length-1;
            int range = isMax?1:(nextMin - rankMins[ri]);
            int prog = isMax?100: Math.round(((xp - rankMins[ri])/(float)range)*100);

            if(heroLevel!=null) heroLevel.setText("LV."+(xp/100+1)+"  -  "+xp+" XP");
            if(rankLabel!=null) rankLabel.setText(rankNames[ri]);
            if(xpLabel!=null) xpLabel.setText(isMax ? xp+" / MAX" : xp+" / "+nextMin+" XP -> "+rankIds[ri+1]);
            if(xpBar!=null){ xpBar.setMax(100); xpBar.setProgress(prog); }
            if(rankBadge!=null){ rankBadge.setText(rank); int[] rankColors={0xFF5a5a6a,0xFF2d8a4e,0xFF2e7bcf,0xFF7c3aed,0xFFe67e22,0xFFffd700}; rankBadge.setBackgroundColor(rankColors[ri]); }

            if(badgesRow!=null){
                badgesRow.removeAllViews();
                int[] rankColors={0xFF5a5a6a,0xFF2d8a4e,0xFF2e7bcf,0xFF7c3aed,0xFFe67e22,0xFFffd700};
                for(int i=0;i<=ri;i++){
                    TextView tv=new TextView(this);
                    tv.setText(rankIds[i]); tv.setTextSize(10); tv.setTextColor(0xFF0e0e1a);
                    tv.setPadding(14,6,14,6); tv.setBackgroundColor(rankColors[i]);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(0,0,8,0); tv.setLayoutParams(lp);
                    badgesRow.addView(tv);
                }
            }
            View tabAll=findViewById(R.id.tabAll);
            View tabActive=findViewById(R.id.tabActive);
            View tabDone=findViewById(R.id.tabDone);
            int activeBg=0xFFc9a227, idleBg=0xFF2a2440;
            if(tabAll!=null) tabAll.setBackgroundColor(filter.equals("all")?activeBg:idleBg);
            if(tabActive!=null) tabActive.setBackgroundColor(filter.equals("active")?activeBg:idleBg);
            if(tabDone!=null) tabDone.setBackgroundColor(filter.equals("done")?activeBg:idleBg);

            if(adapter!=null) adapter.notifyDataSetChanged();
        } catch(Exception e){ android.util.Log.e("QuestLog","render crash",e); }
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
        quests.add(new Quest("q1","Complete portfolio polish",7,false,System.currentTimeMillis()));
        quests.add(new Quest("q2","Solve 3 DSA - arrays + DP",6,false,System.currentTimeMillis()-1000));
        quests.add(new Quest("q3","Read OS notes - paging",4,true,System.currentTimeMillis()-2000));
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
            h.check.setOnCheckedChangeListener(null);
            h.check.setChecked(q.done);
            h.check.setOnCheckedChangeListener((v,checked)->{
                int before=rankIndex(totalXp());
                q.done=checked;
                int after=rankIndex(totalXp());
                save(); render();
                String msg=checked? "+"+q.xp+" XP!":"- "+q.xp+" XP";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if(checked && after>before){
                    Toast.makeText(MainActivity.this, "Rank up! You are now "+rankIds[after]+" - "+rankNames[after], Toast.LENGTH_LONG).show();
                }
            });
            h.deleteBtn.setOnClickListener(v->{ quests.remove(q); save(); render(); });
        }
        class VH extends RecyclerView.ViewHolder{ TextView title,diffPill,xpPill,deleteBtn; CheckBox check; VH(android.view.View v){ super(v); title=v.findViewById(R.id.title); diffPill=v.findViewById(R.id.diffPill); xpPill=v.findViewById(R.id.xpPill); deleteBtn=v.findViewById(R.id.deleteBtn); check=v.findViewById(R.id.checkDone); } }
    }
}
