package com.example.contentproviderproject

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.contentproviderproject.databinding.FragmentContentProviderBinding
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.TextView
import java.lang.Exception


const val REQUEST_CODE = 42

class ContentProviderFragment : Fragment() {
    private var _binding: FragmentContentProviderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContentProviderBinding.inflate(
            inflater, container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ContentProviderFragment()
    }

    private fun checkPermission() {
        context?.let {

            when {
                ContextCompat.checkSelfPermission(it, Manifest.permission.READ_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                            getContact()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                    AlertDialog.Builder(it)
                        .setTitle(getString(R.string.rule_read_contact))
                        .setMessage(getString(R.string.information_permission))
                        .setPositiveButton(getString(R.string.positive_text_btn)) { _, _ ->
                            requestPermission()
                        }
                        .setNegativeButton(getString(R.string.negative_text_btn)) { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
                else -> {
                    requestPermission()
                }
            }

        }
    }


    private fun requestPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.CALL_PHONE),
            REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        getContact()
                } else {
                    context.let {
                        AlertDialog.Builder(it)
                            .setTitle(getString(R.string.rule_read_contact))
                            .setMessage(getString(R.string.information_permission))
                            .setNegativeButton(getString(R.string.negative_text_btn)) { dialog, _ -> dialog.dismiss() }
                            .create()
                            .show()
                    }
                }
                true
            }
        }
    }


    private fun getContact() {

        activity?.let {
            val contentResolver: ContentResolver = it.contentResolver

            val cursorWithContacts: Cursor? = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )

            cursorWithContacts?.let { cursor ->
                for (i in 0..cursor.count) {
                    if (cursor.moveToPosition(i)) {
                        var position = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val name = cursor.getString(position)
                        position = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val id = cursor.getString(position)
                        val number = getNumber(id)
                        addView(it, name, number)
                    }
                }
            }

        cursorWithContacts?.close()
        }

    }

    private fun getNumber(id:String) :String {
        var number = ""

        activity?.let {
            val contentResolver: ContentResolver = it.contentResolver
            val cursorWithNumber: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(id),
                null
            )

            cursorWithNumber?.let { cursor ->
                if (cursor.moveToNext()) {
                    val position = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    number = cursor.getString(position)
                }
            }
            cursorWithNumber?.close()
        }
        return number
    }

    private fun addView(context: Context, textName: String, number: String) {
        binding.containerForContacts.addView(
            AppCompatTextView(context).apply {
                text = textName + " tel:$number"
                textSize = resources.getDimension(R.dimen.text_size_10_sp)
                setOnClickListener {

                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                        PackageManager.PERMISSION_GRANTED) {

                        val number = (it as TextView).text.toString().substringAfter("tel:")
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:$number")
                        try {
                            startActivity(callIntent)
                        } catch (e: Exception) {
                            e.message?.let { it1 -> Log.i("Call", it1) }
                        }
                    } else {
                        requestPermission()
                    }

                }
            }
        )
    }


}