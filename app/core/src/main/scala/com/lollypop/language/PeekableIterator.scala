package com.lollypop.language

/**
 * Peekable Iterator
 * @author lawrence.daniels@gmail.com
 */
class PeekableIterator[+T](values: Seq[T], var position: Int = 0) extends Iterator[T] {
  private var marks: List[Int] = Nil

  /**
   * Returns an option of the token at the given offset
   * @param offset the given offset
   * @return an option of a [[Token token]]
   */
  def apply(offset: Int): Option[T] = peekAhead(offset)

  def commit(): Unit = marks = marks.tail

  def getPosition: Int = position

  /**
   * Returns the option of the index where the given function is satisfied from the current position
   * @param f the given function to satisfy
   * @return the option of the index where the given function is satisfied
   */
  def indexWhereOpt(f: T => Boolean): Option[Int] = {
    (for {
      pos <- position until length
      value = values(pos)
      foundIndex = pos if f(value)
    } yield foundIndex).headOption
  }

  /**
   * Tests whether this iterator can provide another element.
   * @return `true` if a subsequent call to `next` will yield an element,
   *         `false` otherwise.
   */
  override def hasNext: Boolean = position < values.length

  /**
   * Marks the current position within the iterator; allowing one to
   * return to this position via [[rollback()]]
   */
  def mark(): Unit = marks = position :: marks

  /**
   * Returns the next element from the iterator
   * @throws IllegalStateException if the [[hasNext]] returns `false`
   * @return the next [[T element]]
   * @see [[nextOption]]
   */
  @throws[IllegalStateException]
  override def next(): T = {
    // must have more elements
    if (!hasNext)
      throw new IllegalStateException("Out of bounds")

    // return the value
    val value = values(position)
    position += 1
    value
  }

  /**
   * Returns an option of the next element from the iterator
   * @return an option of the next [[T element]]
   */
  override def nextOption(): Option[T] = {
    if (hasNext) {
      val value = Option(values(position))
      position += 1
      value
    } else None
  }

  /**
   * Returns an option of the next element from the iterator without moving the cursor
   * @return an option of the next [[T element]]
   */
  def peek: Option[T] = if (hasNext) Option(values(position)) else None

  /**
   * Returns an option of the element `offset` places head in the iterator without moving the cursor
   * @return an option of the [[T element]]
   */
  def peekAhead(offset: Int): Option[T] =
    if (position + offset >= 0 && position + offset < values.length) Option(values(position + offset)) else None

  def rollback(): this.type = {
    marks.headOption match {
      case Some(markedPos) =>
        position = markedPos
        marks = marks.tail
      case _ =>
    }
    this
  }

  override def toString: String = s"PeekableIterator(${
    values.zipWithIndex.map {
      case (item, n) if n == position => s"[$item]"
      case (item, _) => item
    } mkString ", "
  })"

}
