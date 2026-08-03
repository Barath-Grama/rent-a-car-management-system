# Engineering notes: auditing and rebuilding an inherited codebase

This is the record of taking over
[AbdullahShahid01/Rent-a-Car-Management-System](https://github.com/AbdullahShahid01/Rent-a-Car-Management-System)
— an unmaintained semester project — reading all of it, writing down what was wrong,
and fixing it.

It exists because "I fixed some bugs" is not a claim anyone can check. Every defect
below has a root cause, a way to reproduce it, and, where it was reproduced by running
code, the actual output.

**Starting point:** 24 files, ~4,900 lines, 8 commits, no tests, no CI, no
dependency-managed build. **37 defects found.** All fixed.

---

## Contents

1. [How the audit was done](#1-how-the-audit-was-done)
2. [The five critical defects](#2-the-five-critical-defects)
3. [Major defects](#3-major-defects)
4. [Moderate and minor defects](#4-moderate-and-minor-defects)
5. [The rebuild](#5-the-rebuild)
6. [Mistakes I made](#6-mistakes-i-made)
7. [What I would do next](#7-what-i-would-do-next)

---

## 1. How the audit was done

Read every file first, in dependency order: the five model classes, then the nineteen
Swing screens. No tooling, no static analyser — just reading, because the interesting
defects here were not the kind a linter finds. Two of the five critical ones are
*semantic*: the code does exactly what it says, and what it says is wrong.

Then each suspected defect was reproduced with a throwaway program driving the real
classes, before any of it was changed. That mattered more than it sounds. One
suspicion — that a cascade delete was corrupting unrelated bookings — turned out to be
**wrong**: brute-forcing all 62 possible booking layouts showed the buggy routine
produced correct results at that call site by accident. Without checking, I would have
written a confident and false claim into this document.

The ordering was deliberate: **tests before the rewrite**. The regression suite was
written against the old flat-file behaviour, then the entire persistence layer was
replaced underneath it. All tests stayed green through the swap, which is the only
reason the migration can be claimed to preserve behaviour rather than merely compile.

---

## 2. The five critical defects

### 2.1 Returning a car destroyed the owner's earnings

**`Booking_UnBookCar.java` — money loss on every second rental**

Each `Booking` was serialized with its own complete copy of the `Customer` and the
`Car`, and the `Car` carried its own copy of the `CarOwner`. Those copies froze at the
moment the booking was saved. Returning a car did this:

```java
CarOwner carOwner = last.getCar().getCarOwner();   // a copy from booking time
carOwner.setBalance(carOwner.getBalance() + bill); // adds to a stale balance
carOwner.Update();                                 // writes it over the real one
```

An owner who earned 200 from one car and then returned a second saw the second write
overwrite the first, because the second booking's frozen copy still said the balance
was 0.

**Reproduced** — one owner, two cars at 100/hr and 200/hr, each out for two hours:

```
  unbooked car 1, bill=200, snapshot owner balance was 0
  owner balance in CarOwner.ser = 200
  unbooked car 2, bill=400, snapshot owner balance was 0
  owner balance in CarOwner.ser = 400   (expected 600)
  customer bill in Customer.ser = 400   (expected 600)
```

200 PKR vanished. The customer was under-charged by the same amount.

**Fixed** by re-reading both records from their own files before adjusting them, and
later removed as a possibility entirely: rows now reference rows by id, so there is no
copy to go stale. Guarded by `returnsAccumulate`.

---

### 2.2 Rented cars were offered as available

**`Booking.java:getUnbookedCars()` — comparing objects that could never be equal**

```java
ArrayList<Car> allCars = Car.View();                 // deserialized from Car.ser
ArrayList<Car> bookedCars = Booking.getBookedCars(); // deserialized from Booking.ser
for (int i = 0; i < bookedCars.size(); i++) {
    allCars.remove(bookedCars.get(i));               // never matches anything
}
```

The two lists come from different files, so the same car is two distinct objects.
`Car` overrides no `equals()`, so `ArrayList.remove(Object)` falls back to reference
identity and removes nothing. The method returned every car, always.

**Reproduced** — with car 1 booked: `unbooked ids = 1 2`, expected `2`.

This also made defect 2.3 easy to reach: the Book dialog was gated on this list being
non-empty, so it always opened.

**Fixed** by matching on ID; now a `NOT IN` subquery. Guarded by
`getUnbookedCarsExcludesBookedCars`.

---

### 2.3 Booking an already-rented car threw a NullPointerException

**`Booking_BookCar.java`**

Two variables tracked one decision. The "already booked" branch cleared the object but
not the string the guard actually tested:

```java
} else {
    car = null;                  // cleared
    // CarID left non-null  <-- the guard below still passes
    JOptionPane.showMessageDialog(null, "This car is already booked !");
}
...
if (CarID != null & customerID != null) {
    ... + car.toString() + ...   // NullPointerException
}
```

**Fixed** by clearing both. The real lesson is the shape: two variables representing
one fact will drift. The logic now lives in `RentalService.bookCar`, which returns a
single result.

---

### 2.4 Deleting a booking destroyed a different one

**`Booking.java:Remove()`**

```java
for (int i = 0; i < booking.size() - 1; i++) {      // never sees the last element
    if (booking.get(i).ID == ID) {
        for (int j = i; j < booking.size() - 1; j++) {
            booking.set(j, booking.get(j + 1));      // shifts, never shrinks
        }
    }
}
...
for (int i = 0; i < booking.size() - 1; i++) {       // writes one fewer than it has
    outputStream.writeObject(booking.get(i));
}
```

It never removed anything. It shifted elements left and then wrote one fewer record
than the list held. When the ID was present those two errors cancelled and the result
looked correct. When it was absent, nothing shifted and the write still dropped a
record — **the newest booking, silently.**

**Reproduced** — removing a booking id that does not exist:

```
  bookings before = 2
  bookings after  = 1
```

**Was it reachable?** I checked rather than assumed. The only caller passed IDs read
from the file, so in the shipped application it never fired. Reported as a latent
defect, not an active one.

**Fixed** with an ordinary remove-by-id. Guarded by `removeWithUnknownIdLeavesFileUntouched`
and `removeDeletesOnlyTheRequestedBooking` across every position.

---

### 2.5 An empty text field crashed the program

**`Person.java:isIDvalid()`**

```java
boolean flag = true;
for (int i = 0; i < ID.length(); i++) { ... }   // empty string: loop never runs
if (flag) {
    if (Integer.parseInt(ID) <= 0) { ... }      // parseInt("") throws
}
```

An empty string kept `flag` true and reached `Integer.parseInt`. So did any value above
`Integer.MAX_VALUE`. The exception escaped onto the event dispatch thread from four
screens — including *open Remove Customer, press Remove with the box empty*.

**Reproduced:**

```
  isIDvalid("")            THREW java.lang.NumberFormatException: For input string: ""
  isIDvalid("99999999999") THREW java.lang.NumberFormatException
```

**Fixed** with an explicit empty check and a caught overflow. 13 cases now pin the
boundaries, including `2147483647` valid and `2147483648` not.

---

## 3. Major defects

| # | Defect | Root cause |
|---|---|---|
| 6 | Closing an update window left the whole app permanently disabled | Parent disabled before opening; inner window used `DISPOSE_ON_CLOSE` with no listener to re-enable it. Only escape was killing the process |
| 7 | Cancelling the booking confirmation froze that window | `setEnabled(false)` with no `else` branch. The sibling unbook dialog had one |
| 8 | Logout stacked panels on a disposed frame | `Runner.FRAME` is `static final` and already disposed; each logout added another `Login` panel without clearing. **One logout looked fine — the bug only appears on the second** |
| 9 | "Car Rented" column printed the car id twice | `getID() + ": " + getID()` — copy-paste, second should be `getName()` |
| 10 | One returned booking hid all the active ones | The `else` branch *assigned* "No Cars Booked!" instead of skipping, wiping what had accumulated |
| 11 | Edits never reached the copies held in other files | Same root cause as 2.1. Renaming a customer left every booking showing the old name |
| 12 | Deleting left orphans | No check that a car was out before removing it |
| 13 | Retired IDs came back | `last.ID + 1` — deleting the newest record lowered the maximum |
| 14 | Reg-no search cleared the customer field | Wrong field name in the handler |

Defect 8 is the one worth dwelling on: it is invisible to any test that logs out once.
The regression test does it three times and asserts the component count stays at 1.

Defect 13 has a subtlety I got wrong at first — see [section 6](#6-mistakes-i-made).

---

## 4. Moderate and minor defects

Twenty-two more, summarised:

**Moderate** — no `serialVersionUID` on any persisted class (any field change would
have made every stored record unreadable); the entire UI built off the event dispatch
thread; a build that could not resolve its own classpath outside a configured NetBeans;
credentials as string literals in the source; `"-123"` accepted as a registration
number; all UI state `static`, so a second window silently replaced the first one's
widgets; and roughly 45 error paths that printed to a console no user sees while the
screen reported success regardless.

**Minor** — a row built with 13 values for a 12-column table; two menu items opening the
same file; a duplicated import; 26 leftover debug `println`s; deprecated `Date.getYear()`;
`setLocationRelativeTo(this)` (a window centring on itself) in 8 places; an image loaded
with the wrong filename case; dead switch arms; 18 widgets constructed, sized, coloured
and never added to any window; bitwise `&` where `&&` was meant; an empty picker shown
when there was nothing to pick; billing that truncated part-hours; `remove(i)` inside a
loop without adjusting the index; and assorted unused locals.

**37 — found by looking at the running UI, not by any test.** The booking table
formatted times with `SimpleDateFormat("HH:mm a")`: the 24-hour clock paired with the
am/pm marker. Every afternoon booking since 2019 had rendered as `21:22 pm`. Nothing in
85 tests asserts on a formatted date, and nothing ever would have. It was found by
rendering the screen to a PNG and reading it.

---

## 5. The rebuild

With the defects fixed, four things were structurally wrong and stayed wrong until the
design changed.

### Tests and a build that works anywhere

Maven replacing NetBeans Ant; the layout library that had to be registered in an IDE
moved into the source tree; images moved onto the classpath — they had been loaded
relative to the working directory, so the program showed them only when started from
its own folder and showed nothing at all once packaged as a jar.

85 JUnit tests. Eleven are named regressions, one per critical defect. CI runs them on
every push. Coverage is reported over the model, service and DAO layers and **excludes
the Swing screens deliberately** — they are ~4,000 of the ~6,700 lines here and are not
unit-tested, so including them would report a number that describes a testing decision
while looking like a statement about the code.

### SQLite instead of serialized objects

The schema does what the application had been doing by hand, and does it correctly:

- `booking` references `customer_id` and `car_id` instead of embedding copies —
  **defect 2.1 and 11 become impossible, not fixed**
- `ON DELETE CASCADE` replaces hand-written deletion loops with one atomic step
- `UNIQUE` on `cnic` and `reg_no` replaces manual duplicate checks
- `AUTOINCREMENT` never reissues an id, so the ID-reuse workaround was deleted
- `return_time` is `NULL` while a car is out, rather than `0` — which is also a valid
  epoch millisecond, making "still out" and "returned in 1970" the same value

The committed `.ser` files are imported on first run, ids and all. Verified against the
real data: 3 owners, 3 customers, 4 cars, 3 bookings, ids preserved including their
gaps (customers were 1, 6, 8), the next insert continued at 9, reopening did not import
twice, and an orphan insert was refused by the foreign key.

### A service layer, and one transaction

Bill calculation, owner credit and customer charge had lived in a button handler as
three unrelated writes. Any one could fail alone and leave the books disagreeing.
`RentalService.returnCar` now commits all three together or none.

Proved by a test that credits an owner and then trips a `UNIQUE` violation: the balance
returns to its original value rather than keeping the credit.

### Recording what was charged

Revenue reporting cannot recompute from `car.rent_per_hour`, because that rate is
editable — raising a price would silently rewrite the takings of every rental that car
ever had. `booking.amount_charged` records the figure at the moment of return, with a
migration that adds the column to existing databases.

---

## 6. Mistakes I made

Left in because the corrections are the useful part.

**I broke the committed data and did not notice.** Adding `serialVersionUID = 1L` to
the model classes made every committed `.ser` file unreadable — they carry the UIDs the
*original* classes computed, and a mismatch throws `InvalidClassException`. The whole
suite passed, because nothing read those files. It surfaced only when the importer had
to. The real UIDs were recovered by parsing the `.ser` headers and pinned instead. **A
green suite is evidence about what it covers and nothing else.**

**My first ID-reuse fix fixed nothing.** I changed `last.ID + 1` to `max(existing) + 1`,
which is identical in effect: deleting the highest record lowers the maximum either
way. The probe caught it immediately. It needed a counter that deletion does not touch —
and it mattered more than it first appeared, because bookings now resolve by id, so a
recycled id would re-bind an old booking to a different person.

**Two of my own tests asserted against a boundary they could not hold.** Backdating a
booking by exactly two hours and expecting a two-hour bill fails, because elapsed time
is two hours *plus however long the test took*, and any started hour rounds up. The
code was right; the fixture was wrong.

**I suspected a bug that was not there.** The customer-deletion cascade looked like it
had to corrupt unrelated bookings. Brute-forcing all 62 layouts showed it was correct.
Reported as latent rather than active.

---

## 7. What I would do next

- **The grace period is a constant.** Two hours is baked in, because nothing in this
  program has settings yet and inventing a configuration mechanism for one number would
  be the larger change. A rental desk would want to set it.
- **Retired records keep their CNIC and registration.** Those values cannot be reused,
  which is the right default but should probably be a choice at the point of removal.
- **One connection, one machine.** SQLite and a single shared connection are right for
  a desk in one office and wrong for anything else.
- **The screen tests cover wiring, not appearance.** They press real buttons and check
  what was written and said. Nothing checks that a label is legible, that tab order is
  sane, or that a dialog fits on the screen; defect 37 was found by rendering a screen
  to a PNG and reading it, and nothing automated would catch its like.
