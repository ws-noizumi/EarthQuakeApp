package com.example.noizumi.earthquakeapp;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import rejasupotaro.asyncrssclient.RssFeed;
import rejasupotaro.asyncrssclient.RssItem;

/**
 * Created by noizumi on 2016/09/23.
 */

public class RssSetDBThread implements Runnable {
    RssFeed rssFeed;

    public RssSetDBThread(RssFeed rssFeed){
        this.rssFeed = rssFeed;
    }

    public void run(){
        rssFeed.getTitle();
        rssFeed.getDescription();
        List<RssItem> rssItems = rssFeed.getRssItems();
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);

        // Realmの設定
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Task> taskRealmResults = realm.where(Task.class).findAll();
        int identifier;
        Task task = new Task();

        Log.d("javatest","RssSetDBThread start");

        if (taskRealmResults.max("id") != null) {
            identifier = taskRealmResults.max("id").intValue() + 1;
        } else {
            identifier = 0;
        }

        Date date = new Date();

        for (int i = 0; i < rssItems.size(); i++) {
            RssItem rssItem = rssItems.get(i);
            String dateStr = rssItem.getPubDate();
            if(!dateStr.equals("")) {
                try {
                    date = df.parse(dateStr);
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
                taskRealmResults = realm.where(Task.class).equalTo("date",date).findAll();

                // 合致する日時のレコードが無い場合に追加
                if(taskRealmResults.size() == 0) {
                    task.setId(identifier + i);
                    task.setTitle(rssItem.getTitle());
                    task.setContents(rssItem.getDescription());
                    task.setCategory("地震速報");
                    task.setDate(date);
                    task.setLink(rssItem.getLink().toString());

                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(task);
                    realm.commitTransaction();
                }else{
                    Log.d("javatest","skip:"+rssItem.getTitle());
                }
            }

        }
        realm.close();
    }
}
