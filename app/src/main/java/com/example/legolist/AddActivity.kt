package com.example.legolist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.IOException
import java.lang.Thread.sleep
import javax.xml.parsers.DocumentBuilderFactory

class AddActivity : AppCompatActivity() {
    var ProjectNumber: Editable? = null
    var ProjectName: Editable? = null
    var URL: String? = null
    var good = false
    var wait = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        title = "Add new Lego Project"
        val DbHelper = DatabaseHelper(this@AddActivity)

        try {
            DbHelper.createDataBase()
        } catch (ioe: IOException) {
            throw Error("Problems with creating database")
        }
        try {
            DbHelper.openDataBase()
        } catch (sqle: android.database.SQLException) {
            throw sqle
        }

        val newInventoryBtn = findViewById<Button>(R.id.InsertProject)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        URL = sharedPref.getString("url", "http://fcds.cs.put.poznan.pl/MyWeb/BL/").toString()

        newInventoryBtn.setOnClickListener {

            ProjectName = findViewById<EditText>(R.id.InputProjectName).text
            ProjectNumber = findViewById<EditText>(R.id.InputProjectNumber).text

            if (ProjectName.toString().trim().isNotEmpty() && ProjectNumber.toString().trim().isNotEmpty()) {
                if( !DbHelper.existProject(ProjectNumber.toString().toInt())) {
                    ProjectDownloader().execute()


                    while (wait) {}
                    wait = true
                    if (good){
                        good = false
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Something went wrong ", Toast.LENGTH_LONG).show()
                    }

                }else{
                    Toast.makeText(this, "The $ProjectNumber is already in database ", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this, "Name and number of project is mandatory", Toast.LENGTH_LONG).show()
            }

        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ProjectDownloader : AsyncTask<String, Int, String>() {

        override fun doInBackground(vararg params: String?): String {
            try {
                val url = java.net.URL("$URL$ProjectNumber.xml")
                val xmlDocument: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(InputSource(url.openStream()))
                xmlDocument.documentElement.normalize()
                val items: NodeList = xmlDocument.getElementsByTagName("ITEM")

                val DbHelper = DatabaseHelper(this@AddActivity)
                try {
                    DbHelper.createDataBase()
                } catch (ioe: IOException) {
                    throw Error("Problems with creating database")
                }
                try {
                    DbHelper.openDataBase()
                } catch (sqle: android.database.SQLException) {
                    throw sqle
                }

                for (i in 0 until items.length) {
                    val itemNode: Node = items.item(i)
                    if (itemNode.nodeType == Node.ELEMENT_NODE) {
                        val elem = itemNode as Element
                        val children = elem.childNodes
                        var alt: String = ""
                        var typeID: String = ""
                        var itemID: String = ""
                        var quantityInSet: Int = 0
                        var colorID: String = ""
                        var extra: String = ""

                        for (j in 0 until children.length) {
                            val node = children.item(j)
                            if (node is Element) {
                                when (node.nodeName) {
                                    "ITEMTYPE" -> {
                                        typeID = node.textContent
                                    }
                                    "ITEMID" -> {
                                        itemID = node.textContent
                                    }
                                    "QTY" -> {
                                        quantityInSet = node.textContent.toInt()
                                    }
                                    "COLOR" -> {
                                        colorID = node.textContent
                                    }
                                    "EXTRA" -> {
                                        extra = node.textContent
                                    }
                                    "ALTERNATE" -> {
                                        alt = node.textContent
                                    }
                                }
                            }
                        }
                        if (alt == "N") {
                            DbHelper.addInventoryPart(
                                ProjectNumber.toString().toInt(),
                                typeID,
                                itemID,
                                quantityInSet,
                                0,
                                colorID,
                                extra
                            )
                            DbHelper.getPicture(itemID.toString(), colorID.toString())
                        }
                    }
                }
                DbHelper.addProject(ProjectNumber.toString().toInt(), ProjectName.toString())

            } catch (e: IOException) {
                wait = false
                return "IO Exception"
            }
            good = true
            wait = false
            return "Udalo sie"
        }

    }
}


