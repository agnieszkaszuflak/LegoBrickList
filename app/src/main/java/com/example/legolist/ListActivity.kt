package com.example.legolist

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_list.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.sql.SQLException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        title = "List of Bricks"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val DbHelper = DatabaseHelper(this@ListActivity)
        try {
            DbHelper.createDataBase()
        } catch (ioe: IOException) {
            throw Error("Problems with creating database")
        }
        try {
            DbHelper.openDataBase()
        } catch (sqlE: SQLException) {
            throw sqlE
        }

        val t = intent.extras!!.getString("name")

        DbHelper.updateLastAccessed(intent.extras!!.getInt("id"))

        if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1) title  = "Project $t"
        else title = "Project $t is archived"


        val deleteBtn = findViewById<Button>(R.id.Delete)
        deleteBtn.setOnClickListener {
            val dialog = AlertDialog.Builder(this@ListActivity)
            dialog.setTitle("Archive")
            dialog.setMessage("Would you like to delete this project?")
            dialog.setPositiveButton("Yes") { _, _ ->
                DbHelper.deleteProject(intent.extras!!.getInt("id"))
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            dialog.setNegativeButton("No") { _, _ -> }
            dialog.show()
        }

        val projectsParts = DbHelper.findProjectParts(intent.extras!!.getInt("id"))

        val exportBtn = findViewById<Button>(R.id.Export)
        exportBtn.setOnClickListener {
            val dialog = AlertDialog.Builder(this@ListActivity)
            dialog.setTitle("Export")
            dialog.setMessage("What bricks would you like to export?")
            dialog.setNegativeButton("Used" +
                    "" +
                    "" +
                    "") { _, _ ->
                writeXML(projectsParts, "U")
            }
            dialog.setPositiveButton("New") { _, _ ->
               writeXML(projectsParts, "N")
            }
            dialog.show()
        }

        showInventoriesParts(projectsParts)
    }

    private fun showInventoriesParts(projectParts: MutableList<ProjectPart>) {
        val DbHelper = DatabaseHelper(this@ListActivity)

        try {
            DbHelper.createDataBase()
        } catch (ioe: IOException) {
            throw Error("Problems with creating database")
        }
        try {
            DbHelper.openDataBase()
        } catch (sqlE: SQLException) {
            throw sqlE
        }


        var rows = 0
        rows = projectParts.count()
        for (i in 0 until rows) {
            val row: ProjectPart = projectParts[i]
            var image: ByteArray? = null

            val iv = ImageView(this)
            iv.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT)
            image = DbHelper.findBrickPicture(row.itemID!!, row.colorID!!)

            if (image != null) {
                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(image))
                run {
                    iv.setImageBitmap(bitmap)
                    iv.layoutParams.height = 280
                    iv.layoutParams.width = 280
                }
              }



            val information = DbHelper.findInfo(row.itemID!!, row.colorID!!)
            if (information != "" && image != null) {


                val textView = TextView(this)
                textView.layoutParams = TableRow.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                
                run {
                    textView.textSize = 17F
                    if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1)
                        textView.setTextColor(Color.parseColor("#078640"))
                    else textView.setTextColor(Color.parseColor("#9285E7"))
                    textView.text = information
                }
                
                val textView2 = TextView(this)
                textView2.layoutParams = TableRow.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )

                run {
                    textView2.text = "  ${row.quantityInStore} of ${row.quantityInSet}"
                    textView2.textSize = 17F
                    if (row.quantityInSet == row.quantityInStore) {
                        textView2.setTypeface(null, Typeface.BOLD_ITALIC)
                        if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1)
                            textView.setTextColor(Color.parseColor("#078640"))
                        else textView.setTextColor(Color.parseColor("#9285E7"))

                    }
                    else {
                        textView2.setTypeface(null, Typeface.ITALIC)
                        if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1)
                            textView.setTextColor(Color.parseColor("#078640"))
                        else textView.setTextColor(Color.parseColor("#9285E7"))
                    }
                }

                val layCustomer = LinearLayout(this)
                layCustomer.orientation = LinearLayout.VERTICAL
                layCustomer.setPadding(30, 25, 15, 10)
                layCustomer.addView(textView)

                val linearLay2 = LinearLayout(this)
                linearLay2.orientation = LinearLayout.HORIZONTAL
                linearLay2.setPadding(0, 10, 0, 0)

                if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1) {
                    // Button minus
                    val buttonMinus = Button(this)
                    buttonMinus.layoutParams = TableLayout.LayoutParams(
                        TableLayout.LayoutParams.WRAP_CONTENT,
                        TableLayout.LayoutParams.WRAP_CONTENT
                    )
                    buttonMinus.text = "-"
                    buttonMinus.layoutParams.width = 100
                    buttonMinus.setOnClickListener {
                        val new = DbHelper.changeQuantity(row.id, -1)
                        if (new != -1) textView2.text = "  $new of ${row.quantityInSet}"
                        if (row.quantityInSet != new) {
                            textView2.setTypeface(null, Typeface.ITALIC)
                            if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1)
                                textView.setTextColor(Color.parseColor("#078640"))
                            else textView.setTextColor(Color.parseColor("#9285E7"))

                        }
                    }

                    // Button plus
                    val buttonPlus = Button(this)
                    buttonPlus.layoutParams = TableLayout.LayoutParams(
                        TableLayout.LayoutParams.WRAP_CONTENT,
                        TableLayout.LayoutParams.WRAP_CONTENT
                    )
                    buttonPlus.text = "+"
                    buttonPlus.layoutParams.width = 100
                    buttonPlus.setOnClickListener {
                        val change = DbHelper.changeQuantity(row.id, 1)
                        if (change != -1) textView2.text = "  $change of ${row.quantityInSet}"
                        if (row.quantityInSet == change) {
                            textView2.setTypeface(null, Typeface.BOLD_ITALIC)
                            if (DbHelper.checkIfActive(intent.extras!!.getInt("id")) == 1)
                                textView.setTextColor(Color.parseColor("#078640"))
                            else textView.setTextColor(Color.parseColor("#9285E7"))

                        }
                    }
                    linearLay2.addView(buttonMinus)
                    linearLay2.addView(buttonPlus)
                }

                linearLay2.addView(textView2)
                layCustomer.addView(linearLay2)
                val tableRow = TableRow(this)
                tableRow.id = i + 1
                val layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                tableRow.layoutParams = layoutParams
                tableRow.addView(iv)
                tableRow.addView(layCustomer)
                PartsTable.addView(tableRow, layoutParams)
            }
        }
    }

    private fun writeXML(list: MutableList<ProjectPart>, new: String) {
        val docBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()

        val DbHelper = DatabaseHelper(this@ListActivity)
        try {
            DbHelper.createDataBase()
        } catch (ioe: IOException) {
            throw Error("Problems with creating database")
        }
        try {
            DbHelper.openDataBase()
        } catch (sqlE: SQLException) {
            throw sqlE
        }

        val rootElement: Element = doc.createElement("INVENTORY")

        for (i in 0 until list.count()) {

            val item: Element = doc.createElement("ITEM")
            val itemType: Element = doc.createElement("ITEMTYPE")
            itemType.appendChild(doc.createTextNode(list[i].typeID))
            item.appendChild(itemType)
            val itemID: Element = doc.createElement("ITEMID")
            itemID.appendChild(doc.createTextNode(list[i].itemID))
            item.appendChild(itemID)
            val color: Element = doc.createElement("COLOR")
            color.appendChild(doc.createTextNode(list[i].colorID))
            item.appendChild(color)
            val qtyFilled: Element = doc.createElement("QTYFILLED")
            qtyFilled.appendChild(doc.createTextNode(DbHelper.getQuantity(list[i].id).toString()))
            item.appendChild(qtyFilled)
            val condition: Element = doc.createElement("CONDITION")
            condition.appendChild(doc.createTextNode(new))
            item.appendChild(condition)

            rootElement.appendChild(item)
        }
        doc.appendChild(rootElement)

        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

        val sw = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(sw))

        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val file = File(
                Environment.getExternalStorageDirectory(),
                "${intent.extras!!.getString("name")}.xml"
            )
            try {
                val fileOutput = FileOutputStream(file)
                fileOutput.write(sw.toString().toByteArray())
                fileOutput.close()
                Toast.makeText(this, "File output saved", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
            }
        } else {
            Toast.makeText(this, "File output didn't save", Toast.LENGTH_SHORT).show()
        }
        transformer.transform(DOMSource(doc), StreamResult(System.out))

    }
}

