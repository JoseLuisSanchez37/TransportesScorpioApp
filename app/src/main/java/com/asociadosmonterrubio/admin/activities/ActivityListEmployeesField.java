package com.asociadosmonterrubio.admin.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.asociadosmonterrubio.admin.R;
import com.asociadosmonterrubio.admin.adapters.ListEmployeeAdapter;
import com.asociadosmonterrubio.admin.firebase.FireBaseQuery;
import com.asociadosmonterrubio.admin.utils.SingletonUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ActivityListEmployeesField extends AppCompatActivity implements AdapterView.OnItemClickListener, TextWatcher{

	private ProgressDialog progressDialog;
	private ArrayList<Map<String, String>> employees;
	private ListEmployeeAdapter listEmployeeAdapter;
    private boolean isSpecialField = false;
    private String currentFieldPath = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_employees_field);
		ButterKnife.bind(this);
		employees = new ArrayList<>();
		initLoadingAnActionBar();
		loadEmployeesFromField();
	}

	private void initLoadingAnActionBar(){
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(getString(R.string.subtitle_listado_empleados_campo) + " " + SingletonUser.getInstance().getUsuario().getCampo());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Descargando...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

	@SuppressWarnings("unchecked")
	private void loadEmployeesFromField(){
		String pathGetTemporadaActual = FireBaseQuery.TEMPORADAS_SEDES + "/" + SingletonUser.getInstance().getUsuario().getSede() + "/Temporada_Actual";
		FireBaseQuery.databaseReference.child(pathGetTemporadaActual).addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				String pathEmployees = FireBaseQuery.ASIGNACION_EMPLEADOS_CAMPO + "/" +
						SingletonUser.getInstance().getUsuario().getSede() + "/" +
 						dataSnapshot.getValue().toString() + "/" + //Esta es la temporada actual
						SingletonUser.getInstance().getUsuario().getCampo();
                currentFieldPath = pathEmployees;

				FireBaseQuery.databaseReference.child(pathEmployees).addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot dataSnapshot) {
                        employees.clear();
						for (DataSnapshot children : dataSnapshot.getChildren()){
							Map<String, String> childrenData = (Map<String, String>) children.getValue();
							Map<String, String> employee = new HashMap<>();
							employee.put("ID", children.getKey());
							employee.put("Nombre", childrenData.get("Nombre") != null ? childrenData.get("Nombre") : "");
							employee.put("Apellido_Paterno", childrenData.get("Apellido_Paterno") != null ? childrenData.get("Apellido_Paterno") : "");
							employee.put("Apellido_Materno", childrenData.get("Apellido_Materno") != null ? childrenData.get("Apellido_Materno") : "");
                            if (childrenData.containsKey("IDExterno"))
                                isSpecialField = true;
							employee.put("IDExterno",  childrenData.containsKey("IDExterno") ? childrenData.get("IDExterno") : "");
                            employee.put("pushId", childrenData.get("pushId"));
                            employee.put("Fecha_Nacimiento", childrenData.get("Fecha_Nacimiento"));
                            employee.put("Lugar_Nacimiento", childrenData.get("Lugar_Nacimiento"));
                            employee.put("CURP", childrenData.get("CURP"));
                            employee.put("Actividad", childrenData.get("Actividad"));
                            employee.put("Fecha_Salida", childrenData.containsKey("Fecha_Salida") ? childrenData.get("Fecha_Salida") : "");
                            employee.put("Modalidad", childrenData.containsKey("Modalidad") ? childrenData.get("Modalidad") : "");

							employees.add(employee);
						}

						if (progressDialog != null)
							progressDialog.dismiss();

						if (employees.isEmpty())
							Toast.makeText(ActivityListEmployeesField.this, "La lista de empleados esta vacia", Toast.LENGTH_SHORT).show();
						else {
                            Collections.sort(employees, new Comparator<Map<String, String>>() {
                                @Override
                                public int compare(Map<String, String> map1, Map<String, String> map2) {
                                    return isSpecialField ? map1.get("IDExterno").compareToIgnoreCase(map2.get("IDExterno")) : map1.get("ID").compareToIgnoreCase(map2.get("ID"));
                                }
                            });
                            Collections.reverse(employees);
							listEmployeeAdapter = new ListEmployeeAdapter(ActivityListEmployeesField.this, employees, ListEmployeeAdapter.NONE);
							list_employees.setAdapter(listEmployeeAdapter);
                            list_employees.setOnItemClickListener(ActivityListEmployeesField.this);

                            auto_complete_finder.setAdapter(listEmployeeAdapter);
                            auto_complete_finder.addTextChangedListener(ActivityListEmployeesField.this);
						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError) {
						if (progressDialog != null)
							progressDialog.dismiss();
						onBackPressed();
						Toast.makeText(ActivityListEmployeesField.this, "Ocurrio un error al descargar la lista de empleados", Toast.LENGTH_SHORT).show();
					}
				});
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				if (progressDialog != null)
					progressDialog.dismiss();
				onBackPressed();
				Toast.makeText(ActivityListEmployeesField.this, "Ocurrio un error al descargar la lista de empleados", Toast.LENGTH_SHORT).show();
			}
		});

	}

    @Override
    protected void onRestart() {
        super.onRestart();
        auto_complete_finder.getText().clear();
        refreshSearchList(null);
    }

    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    Intent intent = new Intent(this, ActivityUpdateInfoEmployee.class);
        intent.putExtra("employeeData", (Serializable) employees.get(position));
        intent.putExtra("path", currentFieldPath + "/" + employees.get(position).get("ID"));
        startActivity(intent);
	}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        refreshSearchList(s);
    }

    private void refreshSearchList(CharSequence s){
        if (listEmployeeAdapter != null) {
            if (!TextUtils.isEmpty(s)) {
                auto_complete_finder.dismissDropDown();
                listEmployeeAdapter.getFilter().filter(s);
			}else
                listEmployeeAdapter.updateEmployeesList(employees);
        }
    }

    @Bind(R.id.autocomplete_finder) AutoCompleteTextView auto_complete_finder;
    @Bind(R.id.list_employees) ListView list_employees;
}
