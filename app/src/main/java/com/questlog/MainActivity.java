package com.questlog;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
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

    TextView rankBadge, heroLevel, rankLabel, xpLabel, diffLabel, diffHint;
    TextView statQuests, statXp, statRank, countActive, countDone, streakTop, gemsTop, bannerTitle, bannerSub;
    ProgressBar xpBar;
    LinearLayout badgesRow, ladderRow, diffDots, pathContainer;
    View rankBadgeWrap;
    SeekBar diffSeek;
    EditText questInput;

    String[] rankIds={"E","D","C","B","A","S"};
    int[] rankMins={0,100,300,600,1000,1600};
    String[] rankNames={"E - Novice","D - Apprentice","C - Adept","B - Veteran","A - Master","S - Legend"};
    int[] rankColors={0xFF5a5a6a,0xFF2d8a4e,0xFF1cb0f6,0xFF7c3aed,0xFFff9600,0xFFffd900};

    String[] catIds={"STUDY","CODE","HEALTH","LIFE"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        try {
            setContentView(R.layout.activity_main);
            rankBadge=findViewById(R.id.rankBadge);
            rankBadgeWrap=findViewById(R.id.rankBadgeWrap);
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
            pathContainer=findViewById(R.id.pathContainer);
            streakTop=findViewById(R.id.streakTop);
            gemsTop=findViewById(R.id.gemsTop);
            bannerTitle=findViewById(R.id.bannerTitle);
            bannerSub=findViewById(R.id.bannerSub);
            statQuests=findViewById(R.id.statQuests);
            statXp=findViewById(R.id.statXp);
            statRank=findViewById(R.id.statRank);
            countActive=findViewById(R.id.countActive);
            countDone=findViewById(R.id.countDone);
            diffDots=findViewById(R.id.diffDots);

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
            if(addBtn!=null) addBtn.setOnClickListener(v->{ v.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce)); addQuest(); });
            questInput.setOnEditorActionListener((v,a,e)->{ addQuest(); return true; });
            View tabAll=findViewById(R.id.tabAll), tabActive=findViewById(R.id.tabActive), tabDone=findViewById(R.id.tabDone);
            if(tabAll!=null) tabAll.setOnClickListener(v->setFilter("all"));
            if(tabActive!=null) tabActive.setOnClickListener(v->setFilter("active"));
            if(tabDone!=null) tabDone.setOnClickListener(v->setFilter("done"));
            View clearBtn=findViewById(R.id.clearBtn);
            if(clearBtn!=null) clearBtn.setOnClickListener(v->{ quests.removeIf(q->q.done); save(); render(); Toast.makeText(this,"Cleared!",Toast.LENGTH_SHORT).show(); });
            for(String c: catIds){
                View v=findViewById(getResources().getIdentifier("cat"+cap(c),"id",getPackageName()));
                if(v!=null) v.setOnClickListener(view->{ setCat(c); view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce)); });
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
            v.setBackgroundColor(sel?0xFF58cc02:0xFFe5e5e5);
            if(v instanceof TextView) ((TextView)v).setTextColor(sel?0xFFffffff:0xFF777777);
        }
    }
    void bindDiffDots(){
        if(diffDots==null) return;
        diffDots.removeAllViews();
        for(int i=1;i<=10;i++){
            final int lv=i;
            TextView d=new TextView(this);
            d.setText(String.valueOf(i)); d.setTextSize(10); d.setGravity(Gravity.CENTER);
            d.setPadding(0,10,0,10);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(i>1?6:0,0,0,0); d.setLayoutParams(lp);
            d.setOnClickListener(v->{ selectedDiff=lv; diffSeek.setProgress(lv-1); updateDiffLabel(); highlightDiffDots(); v.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce)); });
            diffDots.addView(d);
        }
        highlightDiffDots();
    }
    void highlightDiffDots(){
        if(diffDots==null) return;
        int[] diffBg={0xFF58cc02,0xFF58cc02,0xFF58cc02,0xFF1cb0f6,0xFF1cb0f6,0xFF1cb0f6,0xFFff9600,0xFFff9600,0xFFff4b4b,0xFFff4b4b};
        for(int i=0;i<diffDots.getChildCount();i++){
            TextView d=(TextView)diffDots.getChildAt(i);
            boolean sel=(i+1)==selectedDiff;
            d.setBackgroundColor(sel?diffBg[i]:0xFFe5e5e5);
            d.setTextColor(sel?0xFFffffff:0xFF777777);
        }
    }

    void updateDiffLabel(){
        if(diffLabel==null) return;
        diffLabel.setText("LV. "+selectedDiff+"  ->  +"+(selectedDiff*10)+" XP");
        String[] hints={"Trivial","Easy","Light","Moderate","Balanced","Tough","Hard","Very Hard","Boss — chest!","BOSS 100 XP!"};
        if(diffHint!=null) diffHint.setText(hints[selectedDiff-1]);
    }

    void addQuest(){
        String t=questInput.getText().toString().trim();
        if(t.isEmpty()){ Toast.makeText(this,"Enter a quest!",Toast.LENGTH_SHORT).show(); return; }
        quests.add(0,new Quest("q"+System.currentTimeMillis(),t,selectedCat,selectedDiff,false,System.currentTimeMillis()));
        questInput.setText(""); save(); render();
        Toast.makeText(this,"Quest LV."+selectedDiff+" +"+(selectedDiff*10)+" XP",Toast.LENGTH_SHORT).show();
    }
    void setFilter(String f){ filter=f; render(); }
    int totalXp(){ int s=0; for(Quest q:quests) if(q.done) s+=q.xp; return s; }
    int rankIndex(int xp){ for(int i=rankMins.length-1;i>=0;i--) if(xp>=rankMins[i]) return i; return 0; }
    int streak(){
        Set<String> days=new HashSet<>();
        for(Quest q:quests) if(q.done){
            java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            days.add(fmt.format(new Date(q.created)));
        }
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
            if(xpBar!=null){
                // animate progress
                ObjectAnimator.ofInt(xpBar, "progress", xpBar.getProgress(), prog).setDuration(600).start();
            }
            if(rankBadge!=null){
                rankBadge.setText(rankIds[ri]);
                if(rankBadgeWrap!=null) rankBadgeWrap.setBackgroundColor(rankColors[ri]);
                else rankBadge.setBackgroundColor(rankColors[ri]);
                rankBadge.setTextColor(ri==5?0xFF1a1433:0xFFffffff);
            }
            if(streakTop!=null) streakTop.setText(String.valueOf(streak()));
            if(gemsTop!=null) gemsTop.setText(String.valueOf(xp));
            if(statQuests!=null) statQuests.setText(String.valueOf(quests.stream().filter(q->q.done).count()));
            if(statXp!=null) statXp.setText(String.valueOf(xp));
            if(statRank!=null) statRank.setText(rankIds[ri]);
            if(countActive!=null) countActive.setText(quests.stream().filter(q->!q.done).count()+" ACTIVE");
            if(countDone!=null) countDone.setText(quests.stream().filter(q->q.done).count()+" DONE");
            if(bannerTitle!=null) bannerTitle.setText("SECTION "+(ri+1)+"  •  "+rankNames[ri]);
            if(bannerSub!=null) bannerSub.setText(isMax?"MAX RANK S — Legend!": (nextMin - xp)+" XP to "+rankIds[ri+1]+"  •  Boss chests glow");

            if(ladderRow!=null){
                ladderRow.removeAllViews();
                for(int i=0;i<rankIds.length;i++){
                    TextView tv=new TextView(this);
                    tv.setText(rankIds[i]); tv.setTextSize(10); tv.setGravity(Gravity.CENTER); tv.setPadding(0,10,0,10);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(i>0?6:0,0,0,0); tv.setLayoutParams(lp);
                    boolean unlocked=xp>=rankMins[i];
                    tv.setBackgroundColor(unlocked?rankColors[i]:0xFFe5e5e5);
                    tv.setTextColor(unlocked? (i==5?0xFF1a1433:0xFFffffff):0xFF999999);
                    tv.setAlpha(unlocked?1f:0.7f);
                    ladderRow.addView(tv);
                }
            }
            if(badgesRow!=null){
                badgesRow.removeAllViews();
                for(int i=0;i<=ri;i++){
                    TextView tv=new TextView(this);
                    tv.setText(rankIds[i]); tv.setTextSize(10); tv.setTextColor(i==5?0xFF1a1433:0xFFffffff);
                    tv.setPadding(18,9,18,9); tv.setBackgroundColor(rankColors[i]);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(0,0,8,0); tv.setLayoutParams(lp);
                    badgesRow.addView(tv);
                }
            }
            View tabAll=findViewById(R.id.tabAll), tabActive=findViewById(R.id.tabActive), tabDone=findViewById(R.id.tabDone);
            int activeBg=0xFF1cb0f6, idleBg=0xFFffffff;
            if(tabAll!=null) tabAll.setBackgroundColor(filter.equals("all")?activeBg:idleBg);
            if(tabActive!=null) tabActive.setBackgroundColor(filter.equals("active")?activeBg:idleBg);
            if(tabDone!=null) tabDone.setBackgroundColor(filter.equals("done")?activeBg:idleBg);
            if(tabAll instanceof TextView) ((TextView)tabAll).setTextColor(filter.equals("all")?0xFFffffff:0xFF777777);
            if(tabActive instanceof TextView) ((TextView)tabActive).setTextColor(filter.equals("active")?0xFFffffff:0xFF777777);
            if(tabDone instanceof TextView) ((TextView)tabDone).setTextColor(filter.equals("done")?0xFFffffff:0xFF777777);

            renderPath();
        } catch(Exception e){ android.util.Log.e("QuestLog","render",e); }
    }

    void renderPath(){
        if(pathContainer==null) return;
        pathContainer.removeAllViews();
        List<Quest> list=new ArrayList<>();
        for(Quest q: quests){ if(filter.equals("active")&&q.done) continue; if(filter.equals("done")&&!q.done) continue; list.add(q); }
        Collections.sort(list,(a,b)-> Long.compare(b.created,a.created));
        LayoutInflater inf=LayoutInflater.from(this);
        int[] wobble={0, 48, -48, 32, -32, 48, -24, 40, -40, 0};

        for(int idx=0; idx<list.size(); idx++){
            Quest q=list.get(idx);
            View row=inf.inflate(R.layout.item_path_node, pathContainer, false);
            LinearLayout nodeCircle=row.findViewById(R.id.nodeCircle);
            ImageView nodeIcon=row.findViewById(R.id.nodeIcon);
            TextView label=row.findViewById(R.id.nodeLabel);
            TextView title=row.findViewById(R.id.nodeTitle);
            View connector=row.findViewById(R.id.connector);
            TextView xpPop=row.findViewById(R.id.xpPop);

            // Winding offset — use padding on row container
            int off=wobble[idx % wobble.length];
            row.setPadding(Math.max(0, off+48), 0, Math.max(0, -off+48), 0);

            boolean isBoss=q.diff>=9;
            boolean isHard=q.diff>=7 && q.diff<=8;
            int bgRes; int iconRes;
            if(q.done){
                bgRes=R.drawable.bg_circle_green; iconRes=R.drawable.ic_node_check;
            } else if(isBoss){
                bgRes=R.drawable.bg_circle_gold; iconRes=R.drawable.ic_node_chest;
            } else if(isHard){
                bgRes=R.drawable.bg_circle_orange; iconRes=R.drawable.ic_node_dumbbell;
            } else {
                bgRes=R.drawable.bg_circle_blue; iconRes=R.drawable.ic_node_star;
            }
            nodeCircle.setBackgroundResource(bgRes);
            nodeCircle.setElevation(q.done?2f:8f);
            nodeIcon.setImageResource(iconRes);
            if(q.done) nodeIcon.setAlpha(1f); else nodeIcon.setAlpha(1f);

            label.setText("LV."+q.diff+"  +"+q.xp+" XP  •  "+q.cat);
            if(q.done){ label.setBackgroundResource(R.drawable.bg_pill_blue); label.setBackgroundColor(0xFF58cc02); label.setTextColor(0xFFffffff); }
            else if(isBoss){ label.setBackgroundResource(R.drawable.bg_pill_gold); label.setTextColor(0xFF1a1433); }
            else { label.setBackgroundResource(R.drawable.bg_pill_blue); label.setTextColor(0xFFffffff); }
            // override with solid after resource
            if(q.done) label.setBackgroundColor(0xFF58cc02);
            else if(isBoss) label.setBackgroundColor(0xFFffd900);
            else label.setBackgroundColor(0xFF1cb0f6);

            title.setText(q.title);
            title.setAlpha(q.done?0.5f:1f);
            if(q.done) title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            else title.setPaintFlags(title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

            if(idx==list.size()-1) connector.setVisibility(View.INVISIBLE);
            else connector.setBackgroundColor(0xFFe5e5e5);

            // Staggered enter animation
            Animation enter=AnimationUtils.loadAnimation(this,R.anim.node_enter);
            enter.setStartOffset(idx*90L);
            row.startAnimation(enter);

            // Tap to toggle with bounce + XP pop
            View.OnClickListener toggle= v->{
                int before=rankIndex(totalXp());
                q.done=!q.done;
                int after=rankIndex(totalXp());
                // XP pop
                if(q.done && xpPop!=null){
                    xpPop.setText("+"+q.xp+" XP");
                    xpPop.setVisibility(View.VISIBLE);
                    Animation rise=AnimationUtils.loadAnimation(MainActivity.this,R.anim.xp_rise);
                    xpPop.startAnimation(rise);
                    new Handler().postDelayed(()->xpPop.setVisibility(View.GONE), 720);
                }
                View circleAnim=nodeCircle;
                circleAnim.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.bounce));
                save(); render();
                Toast.makeText(MainActivity.this, q.done? "+"+q.xp+" XP!":"- "+q.xp+" XP", Toast.LENGTH_SHORT).show();
                if(q.done && after>before){
                    if(rankBadgeWrap!=null) rankBadgeWrap.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.pop));
                    Toast.makeText(MainActivity.this, "★ RANK UP! "+rankIds[after]+" — "+rankNames[after], Toast.LENGTH_LONG).show();
                }
            };
            nodeCircle.setOnClickListener(toggle);
            nodeIcon.setOnClickListener(toggle);
            label.setOnClickListener(toggle);
            title.setOnClickListener(toggle);
            row.setOnLongClickListener(v->{ quests.remove(q); save(); render(); Toast.makeText(this,"Deleted",Toast.LENGTH_SHORT).show(); return true; });

            pathContainer.addView(row);
        }
        if(list.isEmpty()){
            TextView empty=new TextView(this);
            empty.setText("No quests — add one above and it appears on the path.");
            empty.setTextColor(0xFF999999); empty.setTextSize(12); empty.setGravity(Gravity.CENTER);
            empty.setPadding(24,32,24,32);
            pathContainer.addView(empty);
        }
    }

    void save(){
        try{
            JSONArray arr=new JSONArray();
            for(Quest q:quests){ JSONObject o=new JSONObject(); o.put("id",q.id); o.put("title",q.title); o.put("cat",q.cat); o.put("diff",q.diff); o.put("done",q.done); o.put("created",q.created); arr.put(o); }
            getSharedPreferences("questlog",MODE_PRIVATE).edit().putString("quests_v3",arr.toString()).apply();
        }catch(Exception e){}
    }
    void load(){
        try{
            String raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests_v3",null);
            if(raw==null) raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests_v2",null);
            if(raw==null) raw=getSharedPreferences("questlog",MODE_PRIVATE).getString("quests",null);
            if(raw!=null){ JSONArray arr=new JSONArray(raw); for(int i=0;i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); quests.add(new Quest(o.getString("id"),o.getString("title"),o.optString("cat","STUDY"),o.getInt("diff"),o.getBoolean("done"),o.optLong("created",System.currentTimeMillis()))); } return; }
        }catch(Exception e){}
        quests.add(new Quest("q1","Complete portfolio polish", "CODE",7,false,System.currentTimeMillis()));
        quests.add(new Quest("q2","Solve 3 DSA — arrays + DP","CODE",6,false,System.currentTimeMillis()-1000));
        quests.add(new Quest("q3","Water Monstera + mist","LIFE",3,false,System.currentTimeMillis()-2000));
        quests.add(new Quest("q4","Read OS notes — paging","STUDY",4,true,System.currentTimeMillis()-3000));
        quests.add(new Quest("q5","Chest: System design mock","STUDY",10,false,System.currentTimeMillis()-4000));
    }
}
