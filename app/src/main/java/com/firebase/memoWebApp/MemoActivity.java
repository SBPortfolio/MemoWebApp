package com.firebase.memoWebApp;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

public class MemoActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private static FirebaseDatabase firebaseDatabase;
    private EditText content_memo;
    private TextView textName;
    private TextView textEmail;
    private NavigationView navigationView;
    private String selectedMemoKey;
    static {
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        content_memo = (EditText) findViewById(R.id.content_memo);

        if(firebaseUser == null){
            startActivity(new Intent(MemoActivity.this, MainActivity.class));
            finish();
            return;
        }

        FloatingActionButton newMemo = findViewById(R.id.new_memo);
        newMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newMemo();
            }
        });

        FloatingActionButton saveMemo = findViewById(R.id.save_memo);
        saveMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( selectedMemoKey == null){
                    saveMemo();
                } else {
                    updateMemo();
                }

            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        //HeaderView
        View headerView = navigationView.getHeaderView(0);
        textName = (TextView) headerView.findViewById(R.id.textName);
        textEmail = (TextView) headerView.findViewById(R.id.textEmail);

        navigationView.setNavigationItemSelectedListener(this);

        updateProfile();

        displayMemos();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.memo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            deleteMemo();
        } else if (id == R.id.action_logout){
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Memo selectedMemo = (Memo)item.getActionView().getTag();
        content_memo.setText(selectedMemo.getMemoText());
        selectedMemoKey = selectedMemo.getKey();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout(){
        Snackbar.make(content_memo, "로그아웃 하시겠습니까?", Snackbar.LENGTH_LONG).setAction("로그아웃", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                startActivity(new Intent(MemoActivity.this, MainActivity.class));
                finish();
            }
        }).show();
    }

    private void newMemo(){
        selectedMemoKey = null;
        content_memo.setText("");
    }

    private void saveMemo(){
        String text = content_memo.getText().toString();
        if(text.isEmpty()){
            return;
        }
        Memo memo = new Memo();
        memo.setMemoText(content_memo.getText().toString());
        memo.setCreateDate(new Date().getTime());
        firebaseDatabase.getReference("memos/" + firebaseUser.getUid()).push()
                .setValue(memo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MemoActivity.this, "저장 성공", Toast.LENGTH_LONG).show();
                    }
                });

        newMemo();
    }

    private void deleteMemo(){
        if(selectedMemoKey == null){
            return;
        }

        Snackbar.make(content_memo, "메모를 삭제하시겠습니까?", Snackbar.LENGTH_LONG).setAction("삭제", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseDatabase.getReference("memos/" + firebaseUser.getUid() + "/" + selectedMemoKey)
                        .removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                Snackbar.make(content_memo, "삭제 완료", Snackbar.LENGTH_SHORT).show();
                            }
                        });

            }
        }).show();
    }

    private void updateMemo(){
        String text = content_memo.getText().toString();
        if(text.isEmpty()){
            return;
        }

        Memo memo = new Memo();
        memo.setMemoText(content_memo.getText().toString());
        memo.setUpdateDate(new Date().getTime());

        firebaseDatabase.getReference("memos/" + firebaseUser.getUid() + "/" + selectedMemoKey)
                .setValue(memo).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(content_memo, "인증 실패", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile(){
        textName.setText(firebaseUser.getDisplayName());
        textEmail.setText(firebaseUser.getEmail());
    }

    private void displayMemos(){
        firebaseDatabase.getReference("memos/" + firebaseUser.getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Memo memo = dataSnapshot.getValue(Memo.class);
                memo.setKey(dataSnapshot.getKey());
                displayMemosList(memo);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Memo memo = dataSnapshot.getValue(Memo.class);
                memo.setKey(dataSnapshot.getKey());

                for(int i = 0; i < navigationView.getMenu().size(); i++){
                    MenuItem menuItem = navigationView.getMenu().getItem(i);
                    Memo memo1 = (Memo)menuItem.getActionView().getTag();
                    if(memo.getKey().equals(memo1.getKey())){
                        menuItem.getActionView().setTag(memo);
                        menuItem.setTitle(memo.getTitle());
                        break;
                    }
                }

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                newMemo();
                Memo memo = dataSnapshot.getValue(Memo.class);
                memo.setKey(dataSnapshot.getKey());
                for(int i = 0; i < navigationView.getMenu().size(); i++){
                    MenuItem menuItem = navigationView.getMenu().getItem(i);
                    Memo memo1 = (Memo)menuItem.getActionView().getTag();
                    if(memo.getKey().equals(memo1.getKey())){
                        menuItem.setVisible(false);
                        break;
                    }
                }

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayMemosList(Memo memo){
        Menu leftMenu = navigationView.getMenu();
        MenuItem item = leftMenu.add(memo.getTitle());

        View view = new View(getApplication());
        view.setTag(memo);
        item.setActionView(view);
    }
}
