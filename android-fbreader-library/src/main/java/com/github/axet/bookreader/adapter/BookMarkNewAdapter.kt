package com.github.axet.bookreader.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.app.Storage.Bookmark
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.SelectionView
import org.geometerplus.zlibrary.ui.android.R
import org.geometerplus.zlibrary.ui.android.databinding.BookMarkItemsBinding

class BookMarkNewAdapter(private var fb: FBReaderView) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  private var typeHolder = 0
  private var listBookMark = listOf<Storage.Bookmark>()
  var actionItemBookmark: ((Storage.Bookmark) -> Unit)? = null

  @SuppressLint("NotifyDataSetChanged")
  fun setListBookMark(listBookMark: List<Bookmark>) {
    this.listBookMark = listBookMark
    notifyDataSetChanged()
  }

  fun setTypeHolder(typeHolder: Int) {
    this.typeHolder = typeHolder
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if (typeHolder == 0) {
      HolderBookMark(
        BookMarkItemsBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        )
      )
    } else {
      HolderBookNote(
        BookMarkItemsBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        )
      )
    }
  }

  @SuppressLint("SetTextI18n")
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val itemBookMark = listBookMark[position]
    if (holder is HolderBookMark) {
      val textName = if (itemBookMark.type == Storage.TYPE_NOTE) {
        itemBookMark.text.replace("\n", " ")
      } else {
        val textPage =
          itemBookMark.pageBookMark.toString() + "/" + itemBookMark.totalPage.toString()
        val textName = itemBookMark.nameBookMark.replace(
          "\n",
          " "
        )
        if (textPage == textName) {
          itemBookMark.nameBookMark.replace(
            "\n",
            " "
          )
        } else {
          itemBookMark.pageBookMark.toString() + "/" + itemBookMark.totalPage.toString() + ": " + itemBookMark.nameBookMark.replace(
            "\n",
            " "
          )
        }
      }
      if (itemBookMark.type == Storage.TYPE_NOTE) {
        val color = if (itemBookMark.color == 0) {
          fb.app.BookTextView.highlightingBackgroundColor.intValue()
        } else {
          itemBookMark.color
        }
        holder.binding.imgIcon.setBackgroundColor(SelectionView.SELECTION_ALPHA shl 24 or (color and 0xffffff))
      } else {
        holder.binding.imgIcon.setImageResource(R.drawable.ic_toc_black_24dp)
      }
      holder.binding.txtName.text = textName

      holder.binding.root.setOnClickListener {
        actionItemBookmark?.invoke(itemBookMark)
      }
    } else if (holder is HolderBookNote) {

    }
  }

  override fun getItemCount(): Int {
    return listBookMark.size
  }

  override fun getItemViewType(position: Int): Int {
    return typeHolder
  }

  class HolderBookMark(val binding: BookMarkItemsBinding) :
    RecyclerView.ViewHolder(binding.root)

  class HolderBookNote(val binding: BookMarkItemsBinding) :
    RecyclerView.ViewHolder(binding.root)
}
