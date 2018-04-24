package com.example.yousef.seniorproject_cpit499;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference cartDB = FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("user_cart");

    private List<products> productsList;
    private cartListAdapter cartListAdapter;
    private RecyclerView list;

    private double total;
    private String address;

    private Button b;

    static ProgressDialog progressDialog;
    ConnectivityManager connection;
    NetworkInfo netInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        productsList = new ArrayList<>();
        cartListAdapter = new cartListAdapter(this, productsList, cartDB);

        list = (RecyclerView) findViewById(R.id.cartRecyclerView);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(cartListAdapter);

        b = (Button) findViewById(R.id.proceedToCheckout_button);
        retrieveCart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //checking
        connection = (ConnectivityManager) getApplicationContext().getSystemService(this.CONNECTIVITY_SERVICE);
        netInfo = connection.getActiveNetworkInfo();

        //check if internet is available
        if (netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()) {
            connectionDialog();
        }
    }

    public void proceedToCheckout(View view) {
        connection = (ConnectivityManager) getApplicationContext().getSystemService(this.CONNECTIVITY_SERVICE);
        netInfo = connection.getActiveNetworkInfo();
        // internet not available
        if (netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()) {
            connectionDialog();
        }
        // internet is available
        else {
            showProgressDialog();
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get().addOnSuccessListener(this, new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        address = documentSnapshot.getString("address");
                        if (address == null) {
                            startActivity(new Intent(getApplicationContext(), addressActivity.class));
                        } else {
                            //Toast.makeText(CartActivity.this, "address existed", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), checkoutSummaryActivity.class));

                        }
                    } else
                        Toast.makeText(CartActivity.this, "Document dose NOT exist", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    public void retrieveCart() {

        cartDB.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                if (queryDocumentSnapshots.isEmpty()) {
                    b.setEnabled(false);
                    Toast.makeText(CartActivity.this, "Your Cart is Empty", Toast.LENGTH_SHORT).show();
                } else {
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        products productInCart = doc.getDocument().toObject(products.class).getID(doc.getDocument().getId());
                        productsList.add(productInCart);
                        total = total + (productInCart.getPrice() * productInCart.getQuantity());
                        cartListAdapter.notifyDataSetChanged();
                    }
                }
                TextView totalprice = (TextView) findViewById(R.id.totalPrice);
                totalprice.setText(String.valueOf(total) + " SAR");
                //Toast.makeText(getApplicationContext(), productsList.get(2).getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
    }

    public void connectionDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Check Your Internet Connection");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("sitting",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface sitting, int which) {
                        sitting.dismiss();
                        startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                    }
                }).setNegativeButton("Retry",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface retry, int i) {
                        retry.dismiss();
                        onStart();
                    }
                });
        AlertDialog alert = alertDialog.create();
        alert.show();
        Toast.makeText(this, "No Internet connection!", Toast.LENGTH_LONG).show();
    }

}
