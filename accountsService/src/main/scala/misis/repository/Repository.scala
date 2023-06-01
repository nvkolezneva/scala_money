package misis.repository

import misis.model.Account
import scala.concurrent.Future
import scala.collection.mutable

class Repository() {
    private var currentId = 0
    private def getNextId(): Int = {
        currentId += 1
        currentId
    }
    private val accounts = mutable.Map.empty[Int, Account]

    def getAccountKeys(): Iterable[String] = {
        accounts.keys.map(_.toString)
    }

    def getAccountCategoryId(accountId: Int): Option[Int] = {
        accounts.get(accountId).map(_.categoryId)
    }

    def accountExists(accountId: Int): Boolean = {
        accounts.contains(accountId)
    }

    def accountExistsWithIdAndAmount(accountId: Int, amount: Int): Boolean = {
        accounts.get(accountId).exists(_.amount + amount >= 0)
    }

    def getAccount(accountId: Int): Future[Option[Account]] = {
        Future.successful(accounts.get(accountId))
    }

    def createBankAccount(initialAmount: Int = 0): Future[Account] = {
        val bankId = 0
        val bankAccount = Account(bankId, initialAmount, 0)
        accounts += (bankId -> bankAccount)
        Future.successful(bankAccount)
    }

    def createAccount(categoryId: Int, initialAmount: Int = 0): Future[Account] = {
        val newId = getNextId()
        val account = Account(newId, initialAmount, categoryId)
        accounts += (newId -> account)
        Future.successful(account)
    }

    def updateAccount(id: Int, value: Int): Future[Unit] = {
        accounts.get(id) match {
            case Some(account) =>
                if (accounts.get(id).exists(_.amount + value >= 0)) {
                    val updatedAccount = account.update(value)
                    accounts += (id -> updatedAccount)
                    Future.successful(())
                } else {
                    Future.failed(new IllegalArgumentException(s"Account $id does not have enough money"))
                }
            case None =>
                Future.failed(new IllegalArgumentException(s"Account $id does not exist"))
        }
    }
}
