package com.example.legolist

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var c: Cursor? = null
    private var archived: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        title = "List of Lego Projects"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        archived = preference.getBoolean("archived", true)
        try { showProjects() } catch (e: Exception) {}

        // Click Button to Add new project and go to AddActivity
        val newInventoryBtn = findViewById<Button>(R.id.AddInventoryButton)
        newInventoryBtn.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showProjects () {
        val DbHelper = DatabaseHelper(this@MainActivity)
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
        val Projects = DbHelper.findProjects()
        var rows = 0
        rows = Projects.count()

        for (i in 0 until rows) {
            val row: Project = Projects[i]
            if (row.active == 0 && !archived) continue
            val textView = TextView(this)
            textView.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            textView.gravity = Gravity.CENTER_VERTICAL
            textView.height = 130
            textView.width = 500
            textView.setPadding(50, 0, 0, 0)
            run {
                textView.text = "${row.name} - ${row.id}"
                textView.setTextColor(Color.parseColor("#3F51B5"))
            }
            val linearLay = LinearLayout(this)
            linearLay.orientation = LinearLayout.HORIZONTAL
            linearLay.gravity = Gravity.LEFT
            linearLay.addView(textView)

            val tableRow = TableRow(this)
            tableRow.id = i + 1
            val trParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT)

            tableRow.setOnClickListener {

                val intent = Intent(this, ListActivity::class.java)
                intent.putExtra("name", row.name)
                intent.putExtra("id", row.id)
                startActivity(intent)
            }
            tableRow.layoutParams = trParams
            tableRow.setBackgroundColor(Color.parseColor("#B6B0B0"))
            tableRow.addView(linearLay)

            val btn = Button(this)
            btn.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            btn.setBackgroundColor(Color.parseColor("#B6B0B0"))
            btn.gravity = Gravity.CENTER
            if (row.active == 0) btn.setTextColor(Color.parseColor("#726B6B"))
            btn.textSize = 35F
            btn.text = "✕"
            btn.layoutParams.width = 120
            btn.layoutParams.height = 120

            // Button to archive Project
            btn.setOnClickListener {
                if (row.active == 1) {
                    val dialog = AlertDialog.Builder(this@MainActivity)
                    dialog.setTitle("Archive")
                    dialog.setMessage("Would you like to archive this inventory?")
                    dialog.setNegativeButton("No") { _, _ -> }
                    dialog.setPositiveButton("Yes") { _, _ ->
                        DbHelper.archiveProject(row.id)
                        onResume()
                    }
                    dialog.show()
                } else {
                    Toast.makeText(this, "Project is already archived", Toast.LENGTH_LONG).show()
                }
            }

            val linearLay2 = LinearLayout(this)
            linearLay2.orientation = LinearLayout.HORIZONTAL
            linearLay2.gravity = Gravity.END
            linearLay2.addView(btn)
            tableRow.addView(linearLay2)
            ProjectsTable.addView(tableRow, trParams)

            val tableRow1 = TableRow(this)
            val layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.setMargins(0, 0, 0, 0)
            tableRow1.layoutParams = layoutParams
            val textView1 = TextView(this)
            val layoutParams1 = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT)
            layoutParams1.span = 4
            textView1.layoutParams = layoutParams1
            textView1.setBackgroundColor(Color.parseColor("#f0f0f0"))
            textView1.height = 5
            tableRow1.addView(textView1)

            ProjectsTable.addView(tableRow1, layoutParams)

        }

    }

    override fun onResume() {
        super.onResume()
        ProjectsTable.removeAllViews()
        val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        archived = pref.getBoolean("archived", true)
        try { showProjects() } catch (e: Exception) {}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
