package com.questlog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    static class Quest {
        String id, title, cat; int diff, xp; boolean done; long created;
        Quest(String id,String t,String cat,int d,boolean done,long c){this.id=id;this.title=t;this.cat=cat;this.diff=d;this.xp=d*10;this.done=done;this.created=c;}
    }

    List<Quest> quests = new ArrayList<>();
    String filter="all";
    String selectedCat="STUDY";
    int selectedDiff=5;

    RecyclerView recycler;
    QuestAdapter adapter;
    TextView rankBadge, heroLevel, rankLabel, xpLabel, diffLabel, diffHint, streakBadge;
    TextView statQuests, statXp, statRank, countActive, countDone;
    ProgressBar xpBar;
    LinearLayout badgesRow, ladderRow, diffDots;
    SeekBar diffSeek;
    EditText questInput;

    String[] rankIds={"E","D","C","B","A","S"};
    int[] rankMins={0,100,300,600,1000,1600};
    String[] rankNames={"E - Novice","D - Apprentice","C - Adept","B - Veteran","A - Master","S - Legend"};
    int[] rankColors={0xFF5a5a6a,0xFF2d8a4e,0xFF3a7bd5,0xFF7c3aed,0xFFe67e22,0xFFffd700};

    String[] catIds={"STUDY","CODE","HEALTH","LIFE"};
    int[][] catColors={{0xFF3a7bd5,0xFF8ec5ff},{0xFFc9a227,0xFFffe27a},{0xFF2d8a4e,0xFF6fcf8a},{0xFFe67e22,0xFFffb86a}};

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
            ladderRow=findViewById(R.id.ladderRow);
            diffSeek=findViewById(R.id.diffSeek);
            diffLabel=findViewById(R.id.diffLabel);
            diffHint=findViewById(R.id.diffHint);
            questInput=findViewById(R.id.questInput);
            recycler=findViewById(R.id.recycler);
            streakBadge=findViewById(R.id.streakBadge);
            statQuests=findViewById(R.id.statQuests);
            statXp=findViewById(R.id.statXp);
            statRank=findViewById(R.id.statRank);
            countActive=findViewById(R.id.countActive);
            countDone=findViewById(R.id.countDone);
            diffDots=findViewById(R.id.diffDots);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setNestedScrollingEnabled(false);
            adapter=new QuestAdapter();
            recycler.setAdapter(adapter);

            load();
            bindDiffDots();
            diffSeek.setProgress(selectedDiff-1);
            updateDiffLabel();
            diffSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean f){ selectedDiff=p+1; updateDiffLabel(); highlightDiffDots(); }
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
            });
            View addBtn = findViewById(R.id.addBtn);
            if(addBtn!=null) addBtn.setOnClickListener(v->addQuest());
            questInput.setOnEditorActionListener((v,a,e)->{ addQuest(); return true; });
            View tabAll=findViewById(R.id.tabAll), tabActive=findViewById(R.id.tabActive), tabDone=findViewById(R.id.tabDone);
            if(tabAll!=null) tabAll.setOnClickListener(v->setFilter("all"));
            if(tabActive!=null) tabActive.setOnClickListener(v->setFilter("active"));
            if(tabDone!=null) tabDone.setOnClickListener(v->setFilter("done"));
            View clearBtn=findViewById(R.id.clearBtn);
            if(clearBtn!=null) clearBtn.setOnClickListener(v->{ quests.removeIf(q->q.done); save(); render(); Toast.makeText(this,"Cleared!",Toast.LENGTH_SHORT).show(); });
            // category tabs
            for(String c: catIds){
                View v=findViewById(getResources().getIdentifier("cat"+cap(c),"id",getPackageName()));
                if(v!=null) v.setOnClickListener(view->setCat(c));
            }
            updateCatUI();
            render();
        } catch(Exception e){
            android.util.Log.e("QuestLog","onCreate",e);
            TextView tv=new TextView(this); tv.setText("Quest Log error: "+e.getMessage()); tv.setPadding(32,32,32,32); tv.setTextColor(0xFFFF4444); setContentView(tv);
        }
    }
    String cap(String s){ return s.substring(0,1)+s.substring(1).toLowerCase(); }

    void setCat(String c){ selectedCat=c; updateCatUI(); }
    void updateCatUI(){
        for(String c: catIds){
            int id=getResources().getIdentifier("cat"+cap(c),"id",getPackageName());
            View v=findViewById(id);
            if(v==null) continue;
            boolean sel=c.equals(selectedCat);
            v.setBackgroundColor(sel?0xFFc9a227:0xFFe8dcc0);
            if(v instanceof TextView) ((TextView)v).setTextColor(sel?0xFF0e0e1a:0xFF1a1433);
        }
    }
    void bindDiffDots(){
        if(diffDots==null) return;
        diffDots.removeAllViews();
        for(int i=1;i<=10;i++){
            final int lv=i;
            TextView d=new TextView(this);
            d.setText(String.valueOf(i)); d.setTextSize(9); d.setGravity(Gravity.CENTER);
            d.setPadding(0,8,0,8);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(i>1?4:0,0,0,0); d.setLayoutParams(lp);
            d.setOnClickListener(v->{ selectedDiff=lv; diffSeek.setProgress(lv-1); updateDiffLabel(); highlightDiffDots(); });
            diffDots.addView(d);
        }
        highlightDiffDots();
    }
    void highlightDiffDots(){
        if(diffDots==null) return;
        int[] diffBg={0xFF2d8a4e,0xFF2d8a4e,0xFF2d8a4e,0xFFc9a227,0xFFc9a227,0xFFc9a227,0xFFe67e22,0xFFe67e22,0xFFff4444,0xFFff4444};
        for(int i=0;i<diffDots.getChildCount();i++){
            TextView d=(TextView)diffDots.getChildAt(i);
            boolean sel=(i+1)==selectedDiff;
            d.setBackgroundColor(sel?diffBg[i]:0xFF0e0e1a);
            d.setTextColor(sel?0xFFffffff:0xFFf4e4bc);
        }
    }

    void updateDiffLabel(){
        if(diffLabel==null) return;
        diffLabel.setText("LV. "+selectedDiff+"  ->  +"+(selectedDiff*10)+" XP");
        String[] hints={"Trivial","Easy","Light","Moderate","Balanced — solid XP","Tough","Hard","Very Hard","Boss","BOSS — 100 XP!"};
        if(diffHint!=null) diffHint.setText(hints[selectedDiff-1]);
    }

    void addQuest(){
        String t=questInput.getText().toString().trim();
        if(t.isEmpty()){ Toast.makeText(this,"Enter a quest!",Toast.LENGTH_SHORT).show(); return; }
        quests.add(0,new Quest("q"+System.currentTimeMillis(),t,selectedCat,selectedDiff,false,System.currentTimeMillis()));
        questInput.setText(""); save(); render();
        Toast.makeText(this,"Quest LV."+selectedDiff+" +"+(selectedDiff*10)+" XP on clear",Toast.LENGTH_SHORT).show();
    }
    void setFilter(String f){ filter=f; render(); }
    int totalXp(){ int s=0; for(Quest q:quests) if(q.done) s+=q.xp; return s; }
    int rankIndex(int xp){ for(int i=rankMins.length-1;i>=0;i--) if(xp>=rankMins[i]) return i; return 0; }
    int streak(){
        // consecutive days with at least one clear
        Set<String> days=new HashSet<>();
        for(Quest q:quests) if(q.done){
            java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            days.add(fmt.format(new Date(q.created)));
        }
        // naive: size of set as streak for demo
        return Math.min(days.size(), 99);
    }

    void render(){
        try {
            int xp=totalXp();
            int ri=rankIndex(xp);
            int nextMin = ri<rankMins.length-1 ? rankMins[ri+1] : rankMins[ri];
            boolean isMax = ri==rankMins.length-1;
            int range = isMax?1:(nextMin - rankMins[ri]);
            int prog = isMax?100: Math.round(((xp - rankMins[ri])/(float)range)*100);

            if(heroLevel!=null) heroLevel.setText("LV."+(xp/100+1)+"  -  "+xp+" XP");
            if(rankLabel!=null) rankLabel.setText(rankNames[ri]);
            if(xpLabel!=null) xpLabel.setText(isMax ? xp+" / MAX" : xp+" / "+nextMin+"  ->  "+rankIds[ri+1]);
            if(xpBar!=null){ xpBar.setMax(100); xpBar.setProgress(prog); }
            if(rankBadge!=null){ rankBadge.setText(rankIds[ri]); rankBadge.setBackgroundColor(rankColors[ri]); }
            if(streakBadge!=null) streakBadge.setText(streak()+" DAY STREAK");
            if(statQuests!=null) statQuests.setText(String.valueOf(quests.stream().filter(q->q.done).count()));
            if(statXp!=null) statXp.setText(String.valueOf(xp));
            if(statRank!=null) statRank.setText(rankIds[ri]);
            if(countActive!=null) countActive.setText(quests.stream().filter(q->!q.done).count()+" ACTIVE");
            if(countDone!=null) countDone.setText(quests.stream().filter(q->q.done).count()+" CLEARED");

            // ladder
            if(ladderRow!=null){
                ladderRow.removeAllViews();
                for(int i=0;i<rankIds.length;i++){
                    TextView tv=new TextView(this);
                    tv.setText(rankIds[i]); tv.setTextSize(10); tv.setGravity(Gravity.CENTER); tv.setPadding(0,8,0,8);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(i>0?6:0,0,0,0); tv.setLayoutParams(lp);
                    boolean unlocked=xp>=rankMins[i];
                    tv.setBackgroundColor(unlocked?rankColors[i]:0xFF0e0e1a);
                    tv.setTextColor(unlocked? (i==5?0xFF0e0e1a:0xFFffffff):0x66f4e4bc);
                    ladderRow.addView(tv);
                }
            }
            if(badgesRow!=null){
                badgesRow.removeAllViews();
                for(int i=0;i<=ri;i++){
                    TextView tv=new TextView(this);
                    tv.setText(rankIds[i]); tv.setTextSize(10); tv.setTextColor(i==5?0xFF0e0e1a:0xFFffffff);
                    tv.setPadding(18,8,18,8); tv.setBackgroundColor(rankColors[i]);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(0,0,8,0); tv.setLayoutParams(lp);
                    badgesRow.addView(tv);
                }
            }
            View tabAll=findViewById(R.id.tabAll), tabActive=findViewById(R.id.tabActive), tabDone=findViewById(R.id.tabDone);
            int activeBg=0xFFc9a227, idleBg=0xFF2a2440;
            if(tabAll!=null) tabAll.setBackgroundColor(filter.equals("all")?activeBg:idleBg);
            if(tabActive!=null) tabActive.setBackgroundColor(filter.equals("active")?activeBg:idleBg);
            if(tabDone!=null) tabDone.setBackgroundColor(filter.equals("done")?activeBg:idleBg);
            if(tabAll instanceof TextView) ((TextView)tabAll).setTextColor(filter.equals("all")?0xFF0e0e1a:0xFFf4e4bc);
            if(tabActive instanceof TextView) ((TextView)tabActive).setTextColor(filter.equals("active")?0xFF0e0e1a:0xFFf4e4bc);
            if(tabDone instanceof TextView) ((TextView)tabDone).setTextColor(filter.equals("done")?0xFF0e0e1a:0xFFf4e4bc);

            if(adapter!=null) adapter.notifyDataSetChanged();
        } catch(Exception e){ android.util.Log.e("QuestLog","render",e); }
    }

    void save(){
        try{
            JSONArray arr=new JSONArray();
            for(Quest q:quests){ JSONObject o=new JSONObject(); o.put("id",q.id); o.put("title",q.title); o.put("cat",q.cat); o.put("diff",q.diff); o.put("done",q.done); o.put("created",q.created); arr.put(o); }
            getSharedPreferences("questlog",MODE_PRIVATE).edit().putString("quests_v2",arr.toString()).apply();
        }catch(Exception e){}
    }
    void load(){
        try{
            String raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests_v2",null);
            if(raw==null) raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests",null);
            if(raw!=null){ JSONArray arr=new JSONArray(raw); for(int i=0;i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); quests.add(new Quest(o.getString("id"),o.getString("title"),o.optString("cat","STUDY"),o.getInt("diff"),o.getBoolean("done"),o.optLong("created",System.currentTimeMillis()))); } return; }
        }catch(Exception e){}
        quests.add(new Quest("q1","Complete portfolio polish", "CODE",7,false,System.currentTimeMillis()));
        quests.add(new Quest("q2","Solve 3 DSA — arrays + DP","CODE",6,false,System.currentTimeMillis()-1000));
        quests.add(new Quest("q3","Monstera care — water + mist","LIFE",3,false,System.currentTimeMillis()-2000));
        quests.add(new Quest("q4","Read OS notes — paging","STUDY",4,true,System.currentTimeMillis()-3000));
    }

    class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.VH>{
        List<Quest> filtered(){ List<Quest> l=new ArrayList<>(); for(Quest q:quests){ if(filter.equals("active")&&q.done) continue; if(filter.equals("done")&&!q.done) continue; l.add(q);} Collections.sort(l,(a,b)-> Boolean.compare(a.done,b.done)!=0 ? Boolean.compare(a.done,b.done) : Long.compare(b.created,a.created)); return l; }
        @Override public int getItemCount(){ return filtered().size(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p,int t){ return new VH(getLayoutInflater().inflate(R.layout.item_quest,p,false)); }
        @Override public void onBindViewHolder(VH h,int pos){
            Quest q=filtered().get(pos);
            h.title.setText(q.title);
            h.title.setAlpha(q.done?0.45f:1f);
            if(q.done) h.title.setPaintFlags(h.title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            else h.title.setPaintFlags(h.title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            h.diffPill.setText("LV."+q.diff);
            h.xpPill.setText("+"+q.xp+" XP");
            h.catPill.setText(q.cat);
            int[] diffBg={0xFF2d8a4e,0xFF2d8a4e,0xFF2d8a4e,0xFFc9a227,0xFFc9a227,0xFFc9a227,0xFFe67e22,0xFFe67e22,0xFFff4444,0xFFff4444};
            h.diffPill.setBackgroundColor(diffBg[q.diff-1]);
            h.diffPill.setTextColor(q.diff>=7?0xFFffffff:0xFF0e0e1a);
            h.xpPill.setBackgroundColor(0xFF1a1433); h.xpPill.setTextColor(0xFFffd700);
            int catIdx=Arrays.asList(catIds).indexOf(q.cat); if(catIdx<0) catIdx=0;
            h.catPill.setBackgroundColor(catColors[catIdx][0]); h.catPill.setTextColor(0xFFffffff);
            // ring
            h.ring.setText(q.done?"✓":"○");
            h.ring.setBackgroundColor(q.done?0xFF2d8a4e:0xFFffffff);
            h.ring.setTextColor(q.done?0xFFffffff:0xFF1a1433);
            // boss glow
            View card=h.itemView;
            card.setAlpha(q.done?0.85f:1f);
            card.setBackgroundColor(q.done?0xFFd8c9a3:0xFFf4e4bc);
            if(q.diff>=9 && !q.done) card.setBackgroundColor(0xFFfff1c1);
            h.ring.setOnClickListener(v->toggle(q));
            h.deleteBtn.setOnClickListener(v->{ quests.remove(q); save(); render(); });
            card.setOnClickListener(v->toggle(q));
        }
        void toggle(Quest q){
            int before=rankIndex(totalXp());
            q.done=!q.done;
            int after=rankIndex(totalXp());
            save(); render();
            Toast.makeText(MainActivity.this, q.done? "+"+q.xp+" XP!":"- "+q.xp+" XP", Toast.LENGTH_SHORT).show();
            if(q.done && after>before){
                Toast.makeText(MainActivity.this, "★ RANK UP! "+rankIds[after]+" — "+rankNames[after], Toast.LENGTH_LONG).show();
            }
        }
        class VH extends RecyclerView.ViewHolder{ TextView title,diffPill,xpPill,catPill,deleteBtn,ring; VH(View v){ super(v); title=v.findViewById(R.id.title); diffPill=v.findViewById(R.id.diffPill); xpPill=v.findViewById(R.id.xpPill); catPill=v.findViewById(R.id.catPill); deleteBtn=v.findViewById(R.id.deleteBtn); ring=v.findViewById(R.id.ring); } }
    }
}
