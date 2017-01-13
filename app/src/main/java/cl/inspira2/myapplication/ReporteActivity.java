package cl.inspira2.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import net.londatiga.android.bluebamboo.pockdata.PocketPos;
import net.londatiga.android.bluebamboo.util.DataConstants;
import net.londatiga.android.bluebamboo.util.DateUtil;
import net.londatiga.android.bluebamboo.util.FontDefine;
import net.londatiga.android.bluebamboo.util.Printer;
import net.londatiga.android.bluebamboo.util.Util;

import java.text.NumberFormat;

public class ReporteActivity extends AppCompatActivity {
    EditText CORRELATIVO_INICIAL;
    EditText CORRELATIVO_FINAL;
    TableLayout GRID_LAYOUT;
    BluetoothPrinterService printer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        CORRELATIVO_INICIAL = (EditText) findViewById(R.id.editText_correlativoInicio);
        CORRELATIVO_FINAL = (EditText) findViewById(R.id.editText_correlativoFin);
        GRID_LAYOUT = (TableLayout) findViewById(R.id.grid);

        DatabaseOperations DOP = new DatabaseOperations(this);
        Cursor min_max = DOP.getReportMinMax();
        if (min_max.moveToFirst()) {
            CORRELATIVO_INICIAL.setText(min_max.getString(min_max.getColumnIndex("min")).replace(",",""));
            CORRELATIVO_FINAL.setText(min_max.getString(min_max.getColumnIndex("max")).replace(",",""));
        }

        printer = ((cBaseApplication)this.getApplicationContext()).printer;
        if(!printer.conected()){
            printer.connect(this);
        }

    }

    public void getReporte(View view) {
        DatabaseOperations DOP = new DatabaseOperations(this);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        String text1 = nf.format(Double.valueOf(CORRELATIVO_INICIAL.getText().toString()));
        String text2 = nf.format(Double.valueOf(CORRELATIVO_FINAL.getText().toString()));
        //Cursor reporte = DOP.getReporte(CORRELATIVO_INICIAL.getText().toString(), CORRELATIVO_FINAL.getText().toString());
        Cursor reporte = DOP.getReporte(text1, text2);
        if (reporte.moveToFirst()) {
            Double totalKilos = 0.0;
            Integer totalCajas = 0;
            GRID_LAYOUT.removeAllViews();
            TableRow primera = new TableRow(this);
            TextView etiqueta1 = new TextView(this);
            TextView etiqueta2 = new TextView(this);
            TextView etiqueta3 = new TextView(this);
            TextView etiqueta4 = new TextView(this);
            etiqueta1.setText("Cuartel");
            etiqueta2.setText("Unidad");
            etiqueta3.setText("N Cajas");
            etiqueta4.setText("Kg Neto");
            primera.addView(etiqueta1);
            primera.addView(etiqueta2);
            primera.addView(etiqueta3);
            primera.addView(etiqueta4);
            GRID_LAYOUT.addView(primera);
            do {
                TableRow row = new TableRow(this);
                for (int j = 0; j < reporte.getColumnCount(); j++) {
                    TextView actualData = new TextView(this);
                    //set properties
                    actualData.setText(reporte.getString(j));
                    row.addView(actualData);
                }
                GRID_LAYOUT.addView(row);
                totalCajas = totalCajas + Integer.parseInt(reporte.getString(2));
                totalKilos = totalKilos + Double.parseDouble(reporte.getString(3));
            } while (reporte.moveToNext());
            TableRow totalc = new TableRow(this);
            TextView a = new TextView(this);
            TextView b = new TextView(this);
            TableRow totalk = new TableRow(this);
            TextView c = new TextView(this);
            TextView d = new TextView(this);
            a.setText("Total Cajas:");
            b.setText(totalCajas.toString());
            c.setText("Total Kilos:");
            d.setText(totalKilos.toString());
            totalc.addView(a);
            totalc.addView(b);
            totalk.addView(c);
            totalk.addView(d);
            GRID_LAYOUT.addView(totalc);
            GRID_LAYOUT.addView(totalk);
        } else {
            GRID_LAYOUT.removeAllViews();
            TableRow primera = new TableRow(this);
            TextView etiqueta1 = new TextView(this);
            TextView etiqueta2 = new TextView(this);
            TextView etiqueta3 = new TextView(this);
            TextView etiqueta4 = new TextView(this);
            etiqueta1.setText("Cuartel");
            etiqueta2.setText("Unidad");
            etiqueta3.setText("N Cajas");
            etiqueta4.setText("Kg Neto");
            primera.addView(etiqueta1);
            primera.addView(etiqueta2);
            primera.addView(etiqueta3);
            primera.addView(etiqueta4);
            GRID_LAYOUT.addView(primera);
            Toast.makeText(this, "No existen capturas entre los correlativos ingresados", Toast.LENGTH_SHORT).show();
        }
        reporte.close();
    }

    public void printReport(View view) {
        DatabaseOperations DOP = new DatabaseOperations(this);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        String text1 = nf.format(Double.valueOf(CORRELATIVO_INICIAL.getText().toString()));
        String text2 = nf.format(Double.valueOf(CORRELATIVO_FINAL.getText().toString()));

        if (printer.conected()) {
            try {
                long milis = System.currentTimeMillis();
                String date = DateUtil.timeMilisToString(milis, "MMM dd, yyyy");
                String time = DateUtil.timeMilisToString(milis, "hh:mm a");
                StringBuffer contentBuffer = new StringBuffer(100);

                contentBuffer.append("\n\nREPORTE PARCIAL\n");
                contentBuffer.append(Util.nameLeftValueRightJustify(date, time, DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("Desde:", CORRELATIVO_INICIAL.getText().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("Hasta:", CORRELATIVO_FINAL.getText().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append("------------------------------\n");
                contentBuffer.append("CUARTEL-UNIDAD-N CAJAS-KG NETO\n");
                contentBuffer.append("------------------------------\n");
                Double totalKilos = 0.0;
                Integer totalCajas = 0;
                Cursor reporte = DOP.getReporte(text1, text2);
                if (reporte.moveToFirst()) {
                    do {
                        contentBuffer.append(reporte.getString(0) + " - " + reporte.getString(1) + " - " + reporte.getString(2) + " - " + reporte.getString(3) + "\n");
                        totalCajas += Integer.parseInt(reporte.getString(2));
                        totalKilos += Double.parseDouble(reporte.getString(3));
                    } while (reporte.moveToNext());
                }else{
                    contentBuffer.append("\n\n- NO HAY CAPTURAS -\n\n");
                }
                reporte.close();
                contentBuffer.append("------------------------------\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("TOTAL CAJAS:", totalCajas.toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("TOTAL KILOS:", totalKilos.toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append("** Agrontrol **\n");

                String receiptContent = contentBuffer.toString();


                byte[] content = Printer.printfont(receiptContent + "\n\n\n\n\n\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN2);
                byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, content, 0, content.length);

                printer.write(senddata);
                GRID_LAYOUT.removeAllViewsInLayout();
                // tell the user data were sent
                Toast.makeText(this, "Reporte impreso.", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ocurrio un error al Imprimir" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay impresora Configurada", Toast.LENGTH_SHORT).show();
        }
    }
}
