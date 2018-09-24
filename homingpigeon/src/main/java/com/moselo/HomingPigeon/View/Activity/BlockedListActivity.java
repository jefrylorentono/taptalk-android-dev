package com.moselo.HomingPigeon.View.Activity;

import android.arch.lifecycle.ViewModelProviders;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.moselo.HomingPigeon.Helper.OverScrolled.OverScrollDecoratorHelper;
import com.moselo.HomingPigeon.Model.UserModel;
import com.moselo.HomingPigeon.R;
import com.moselo.HomingPigeon.View.Adapter.ContactListAdapter;
import com.moselo.HomingPigeon.ViewModel.ContactListViewModel;

public class BlockedListActivity extends BaseActivity {

    ImageView ivButtonBack;
    RecyclerView rvBlockedList;

    ContactListAdapter adapter;

    ContactListViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_list);

        initViewModel();
        initView();
    }

    private void initViewModel() {
        vm = ViewModelProviders.of(this).get(ContactListViewModel.class);

        //Dummy Contacts
        if (vm.getContactList().size() == 0) {
            UserModel u1 = new UserModel("u1", "Bambang 1");
            UserModel u2 = new UserModel("u2", "Bambang 2");
            UserModel u3 = new UserModel("u3", "Bambang 3");
            vm.getFilteredContacts().add(u1);
            vm.getFilteredContacts().add(u2);
            vm.getFilteredContacts().add(u3);
        }
        //End Dummy
    }

    @Override
    protected void initView() {
        getWindow().setBackgroundDrawable(null);

        ivButtonBack = findViewById(R.id.iv_button_back);
        rvBlockedList = findViewById(R.id.rv_blocked_list);

        adapter = new ContactListAdapter(ContactListAdapter.NONE, vm.getFilteredContacts());
        rvBlockedList.setAdapter(adapter);
        rvBlockedList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        OverScrollDecoratorHelper.setUpOverScroll(rvBlockedList, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        ivButtonBack.setOnClickListener(v -> onBackPressed());
    }
}