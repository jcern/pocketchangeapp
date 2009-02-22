package com.pocketchangeapp.snippet

import scala.xml._
import net.liftweb._
import http._
import util._
import S._
import SHtml._
import scala.xml._
import Helpers._

import com.pocketchangeapp.model._
import com.pocketchangeapp.util.Util

import java.util.Date

import net.lag.logging.Logger

/* date | desc | tags | value */ 
class AddEntry extends StatefulSnippet {
  def dispatch = {
    case "addentry" => add _
  }

  var account : Long = _
  var date = ""
  var desc = ""
  var value = ""
  var tags = S.param("tag") openOr ""
  
  def add(in: NodeSeq): NodeSeq = User.currentUser match {
    case Full(user) if user.editable.size > 0 => {

      def doTagsAndSubmit(t: String) {
	tags = t
	if (tags.trim.length == 0) error("We're going to need at least one tag.")
	else {
          /* Get the date correctly, add the datepicker: comes in as yyyy/mm/dd */
	  val txDate = Util.slashDate.parse(date)

	  val amount = BigDecimal(value)

	  // We need to determine the last serial number and balance for the date in question
	  val (txSerial,txBalance) = Transaction.getLastEntryData(txDate)
	  
	  val e = Transaction.create.account(account).dateOf(txDate).serialNumber(txSerial + 1)
	           .description(desc).amount(BigDecimal(value)).tags(tags)
		   .currentBalance(txBalance + amount)

	  e.validate match {
            case Nil => {
	      Transaction.updateEntries(txSerial + 1, amount)
              e.save
  	      val acct = Account.find(account).open_!
	      val newBalance = acct.balance.is + e.amount.is
	      acct.balance(newBalance).save
              notice("Entry added!")
	      unregisterThisSnippet() // dpp: remove the statefullness of this snippet
	    }
            case x => error(x)
	  }
	}
      }

      bind("e", in, 
	   "account" -> select(user.editable.map(acct => (acct.id.toString, acct.name)), Empty, id => account = id.toLong),
	   "dateOf" -> text(Util.slashDate.format(new Date()).toString, date = _) % ("id" -> "entrydate"),
	   "desc" -> text("Item Description", desc = _),
	   "value" -> text("Value", value = _),
	   "tags" -> text(tags, doTagsAndSubmit))
    }
    case _ => Text("")
  }
}
 
