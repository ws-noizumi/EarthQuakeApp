package com.example.noizumi.earthquakeapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.ArrayList;
import org.apache.http.Header;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import rejasupotaro.asyncrssclient.AsyncRssClient;
import rejasupotaro.asyncrssclient.AsyncRssResponseHandler;
import rejasupotaro.asyncrssclient.RssFeed;
import rejasupotaro.asyncrssclient.RssItem;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "com.example.noizumi.taskapp.TASK";
    private SearchView searchView;
    private MenuItem searchItem;
    private String searchWords = "";

    private Realm mRealm;
    private RealmResults<Task> mTaskRealmResults;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange() {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(searchWords);
        actionBar.setDisplayShowTitleEnabled(true);

        if (searchWords.equals("")) {
            mTaskRealmResults = mRealm.where(Task.class).findAll();
        } else {
            mTaskRealmResults = mRealm.where(Task.class).equalTo("category", searchWords).findAll();
        }
        mTaskRealmResults.sort("date", Sort.DESCENDING);
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task);

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.clear();
                        mRealm.commitTransaction();

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        AsyncRssClient client = new AsyncRssClient();
        client.read("http://weather.livedoor.com/forecast/rss/earthquake.xml", new AsyncRssResponseHandler() {
            @Override
            public void onSuccess(RssFeed rssFeed) {
                rssFeed.getTitle();
                rssFeed.getDescription();

                RssItem rssItem = rssFeed.getRssItems().get(0);
                rssItem.getTitle();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                  Throwable error) {
                Log.d("javatest","fail");
            }
        });

        reloadListView();
    }


    private void viewSearchResultList(String subTitle){

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(subTitle);
        actionBar.setDisplayShowTitleEnabled(true);

        searchWords = subTitle;
        if(subTitle.equals("")){
            mTaskRealmResults = mRealm.where(Task.class).findAll();
        }else{
            mTaskRealmResults = mRealm.where(Task.class).equalTo("category",subTitle).findAll();
        }

        mTaskRealmResults.sort("date", Sort.DESCENDING);
        reloadListView();
    }

    private void reloadListView() {

        ArrayList<Task> taskArrayList = new ArrayList<>();

        for (int i = 0; i < mTaskRealmResults.size(); i++) {
            Task task = new Task();

            task.setId(mTaskRealmResults.get(i).getId());
            task.setTitle(mTaskRealmResults.get(i).getTitle());
            task.setCategory(mTaskRealmResults.get(i).getCategory());
            task.setContents(mTaskRealmResults.get(i).getContents());
            task.setDate(mTaskRealmResults.get(i).getDate());

            taskArrayList.add(task);
        }

        mTaskAdapter.setTaskArrayList(taskArrayList);
        mListView.setAdapter(mTaskAdapter);
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

    // SearchViewのリスナー
    private final SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        /*
         * 文字に変更があったら（１文字ずつ呼ばれる)
         *
         * @see android.support.v7.widget.SearchView.OnQueryTextListener#
         * onQueryTextChange(java.lang.String)
         */

        @Override
        public boolean onQueryTextChange(String newText) {
            return true;
        }

        /*
         * 文字入力を確定した場合
         *
         * @see android.support.v7.widget.SearchView.OnQueryTextListener#
         * onQueryTextSubmit(java.lang.String)
         */
        @Override
        public boolean onQueryTextSubmit(String query) {
            viewSearchResultList(query);
            // SearchViewを隠す
            searchView.setQuery("", false);
            searchView.setIconified(true);
            searchItem.collapseActionView();
            // Focusを外す
            searchView.clearFocus();
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // menu定義のitemのidがaction_searchのものを取得する
        searchItem = menu.findItem(R.id.action_search);

        // SearchViewを取得する
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        // 虫メガネのアイコンを表示するか
        searchView.setIconifiedByDefault(true);

        // Submitボタンを表示するか
        searchView.setSubmitButtonEnabled(false);

        // SearchViewに何も入力していない時のテキストを設定
        searchView.setQueryHint("カテゴリ検索");

        // リスナーを登録する
        searchView.setOnQueryTextListener(mOnQueryTextListener);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_reset:
                viewSearchResultList("");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}