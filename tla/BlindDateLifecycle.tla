---------------- MODULE BlindDateLifecycle ----------------
EXTENDS Naturals, Sequences, FiniteSets, TLC
CONSTANTS Sessions, Items, Workers, Resets
(* Source baseline: main 39c1697f. Relative to blinddate/:
 executor/BlindDateEventQueue.java:49-89,102-129,141-147,205-221,233-245;
 handler/BlindDateChoiceHandler.java:33-78;
 scheduler/BlindDateSessionSchedulerImpl.java:151-176;
 service/BlindDateServiceImpl.java:80-85,95-109;
 repository/BlindDateSessionStorageImpl.java:69-88,119-127.
 Queue monitor critical sections are atomic transitions (no blocking acquisition
 while holding another modelled resource). Task bodies have separate Start and
 Finish phases outside it, holding exactly one worker and one serial lane.
 Serial scheduling bookkeeping is atomic at these boundaries.
 The map tracks executor membership; detached lanes remain until their queued
 barriers finish. With one open per UUID and no new UUID creation, pending
 cleanup prevents replacement executors, so snapshot conditional removal is
 equivalent to clearing membership. Executor identity/replacement is omitted.
 Barrier future completion and its wrapper tail are combined: the tail has no
 business effects, but actual cleanup can overlap that scheduling tail. No condition
 variables, explicit unwind locks or allocation failures: Java synchronized
 releases on exceptions; healthy executors and no fatal errors assumed.
 Each initial unique UUID is processing. Its scheduler attempts opening once;
 no UUID reuse/new participant creation during this bounded lifecycle. Timeout
 may run any time after successful open, including before any choice. Requests
 may be rejected, numbered by invocation rather than member identity.
 Finish represents all synchronous external effects returning normally OR by
 caught exception; no claim each job creates a match. No hung calls, shutdown,
 rejection, matching logic, timer wall-clock bound or separate session-kill path.
 Weak fairness for all finite callers, dispatch, task completion and cleanup.
 Reset clear is atomic abstraction: after barriers no old session job can run;
 intermediate per-session storage removals have no additional old-job effects.
 *)
S == 1..Sessions
Job(k,i) == [kind |-> k, id |-> i]
None == Job("idle",0)
VARIABLES opened, alive, openpc, requestpc, timeoutpc, lanes, active,
          mapped, accepted, completed, finalized, issued, cleaned, pending,
          barriers, cleanpc, drained, late
callers == <<opened,alive,openpc,requestpc,timeoutpc,mapped>>
jobs == <<lanes,active,accepted,completed,finalized,barriers,late>>
cleaner == <<issued,cleaned,pending,cleanpc,drained>>
vars == <<callers,jobs,cleaner>>
Init == /\ opened = {} /\ alive = S /\ mapped = {}
        /\ openpc = [s \in S |-> "new"]
        /\ requestpc = [s \in S |-> 0]
        /\ timeoutpc = [s \in S |-> "absent"]
        /\ lanes = [s \in S |-> <<>>]
        /\ active = [s \in S |-> None]
        /\ accepted = [s \in S |-> <<>>]
        /\ completed = [s \in S |-> <<>>]
        /\ finalized = {}
        /\ barriers = {}
        /\ issued = 0 /\ cleaned = 0 /\ pending = 0
        /\ cleanpc = "wait" /\ drained = TRUE /\ late = FALSE
CanOpen(s) == pending = 0 /\ s \in alive
Open(s) == /\ openpc[s] = "new"
           /\ openpc' = [openpc EXCEPT ![s] = "done"]
           /\ opened' = IF CanOpen(s) THEN opened \cup {s} ELSE opened
           /\ timeoutpc' = [timeoutpc EXCEPT ![s] = IF CanOpen(s) THEN "ready" ELSE "done"]
           /\ UNCHANGED <<alive,requestpc,mapped,jobs,cleaner>>
Submit(s) == /\ openpc[s] = "done" /\ requestpc[s] < Items
             /\ LET n == requestpc[s]+1 IN
                /\ requestpc' = [requestpc EXCEPT ![s] = n]
                /\ lanes' = [lanes EXCEPT ![s] = IF s \in opened THEN Append(@,Job("choice",n)) ELSE @]
                /\ accepted' = [accepted EXCEPT ![s] = IF s \in opened THEN Append(@,n) ELSE @]
             /\ mapped' = IF s \in opened THEN mapped \cup {s} ELSE mapped
             /\ UNCHANGED <<opened,alive,openpc,timeoutpc,active,completed,finalized,barriers,late,cleaner>>
Timeout(s) == /\ timeoutpc[s] = "ready"
              /\ timeoutpc' = [timeoutpc EXCEPT ![s] = "done"]
              /\ opened' = opened \ {s}
              /\ lanes' = [lanes EXCEPT ![s] = IF s \in opened THEN Append(@,Job("final",0)) ELSE @]
              /\ mapped' = IF s \in opened THEN mapped \cup {s} ELSE mapped
              /\ UNCHANGED <<alive,openpc,requestpc,active,accepted,completed,finalized,barriers,late,cleaner>>
IssueReset == /\ issued < Resets
              /\ issued' = issued + 1 /\ pending' = pending + 1
              /\ opened' = {}
              /\ lanes' = [s \in S |-> IF s \in mapped THEN Append(lanes[s],Job("barrier",issued+1)) ELSE lanes[s]]
              /\ barriers' = barriers \cup {<<issued+1,s>> : s \in S \ mapped}
              /\ UNCHANGED <<alive,openpc,requestpc,timeoutpc,mapped,active,accepted,completed,finalized,late,cleaned,cleanpc,drained>>
Busy == {s \in S : active[s] # None}
Start(s) == /\ active[s] = None /\ Len(lanes[s]) > 0
            /\ Cardinality(Busy) < Workers
            /\ active' = [active EXCEPT ![s] = Head(lanes[s])]
            /\ lanes' = [lanes EXCEPT ![s] = Tail(@)]
            /\ UNCHANGED <<callers,accepted,completed,finalized,barriers,late,cleaner>>
Finish(s) == /\ active[s] # None
             /\ LET j == active[s] IN
                /\ completed' = [completed EXCEPT ![s] = IF j.kind = "choice" THEN Append(@,j.id) ELSE @]
                /\ finalized' = IF j.kind = "final" THEN finalized \cup {s} ELSE finalized
                /\ alive' = IF j.kind = "final" THEN alive \ {s} ELSE alive
                /\ mapped' = IF j.kind = "final" THEN mapped \ {s} ELSE mapped
                /\ barriers' = IF j.kind = "barrier" THEN barriers \cup {<<j.id,s>>} ELSE barriers
                /\ late' = (late \/ (j.kind = "choice" /\ s \notin alive))
             /\ active' = [active EXCEPT ![s] = None]
             /\ UNCHANGED <<opened,openpc,requestpc,timeoutpc,lanes,accepted,cleaner>>
Clear == /\ cleanpc = "wait" /\ cleaned < issued
         /\ \A s \in S : <<cleaned+1,s>> \in barriers
         /\ cleanpc' = "complete"
         /\ alive' = {}
         /\ drained' = (drained /\ (accepted = completed))
         /\ UNCHANGED <<opened,openpc,requestpc,timeoutpc,mapped,jobs,issued,cleaned,pending>>
Complete == /\ cleanpc = "complete"
            /\ cleanpc' = "wait" /\ cleaned' = cleaned + 1
            /\ pending' = pending - 1 /\ mapped' = {}
            /\ UNCHANGED <<opened,alive,openpc,requestpc,timeoutpc,jobs,issued,drained>>
Terminal == /\ issued = Resets /\ cleaned = Resets /\ pending = 0
            /\ \A s \in S : openpc[s] = "done" /\ requestpc[s] = Items
                        /\ timeoutpc[s] = "done" /\ active[s] = None /\ lanes[s] = <<>>
Next == (\E s \in S : Open(s) \/ Submit(s) \/ Timeout(s) \/ Start(s) \/ Finish(s))
        \/ IssueReset \/ Clear \/ Complete \/ (Terminal /\ UNCHANGED vars)
Spec == Init /\ [][Next]_vars
        /\ (\A s \in S : WF_vars(Open(s)) /\ WF_vars(Submit(s)) /\ WF_vars(Timeout(s))
                       /\ WF_vars(Start(s)) /\ WF_vars(Finish(s)))
        /\ WF_vars(IssueReset) /\ WF_vars(Clear) /\ WF_vars(Complete)
Jobs == {None,Job("final",0)} \cup {Job("choice",i) : i \in 1..Items}
        \cup {Job("barrier",r) : r \in 1..Resets}
TypeOK == /\ lanes \in [S -> Seq(Jobs)] /\ active \in [S -> Jobs]
          /\ accepted \in [S -> Seq(1..Items)] /\ completed \in [S -> Seq(1..Items)]
          /\ openpc \in [S -> {"new","done"}]
          /\ timeoutpc \in [S -> {"absent","ready","done"}]
          /\ cleanpc \in {"wait","complete"}
          /\ mapped \subseteq S /\ finalized \subseteq S
          /\ barriers \subseteq ((1..Resets) \X S)
          /\ drained \in BOOLEAN /\ late \in BOOLEAN
          /\ pending \in 0..Resets /\ issued \in 0..Resets /\ cleaned \in 0..issued
          /\ pending = issued - cleaned /\ Cardinality(Busy) <= Workers
          /\ opened \subseteq S /\ alive \subseteq S
          /\ \A s \in S : requestpc[s] \in 0..Items
Prefix(a,b) == Len(a) <= Len(b) /\ a = SubSeq(b,1,Len(a))
FIFOOnce == \A s \in S : Prefix(completed[s],accepted[s])
QueuedChoices(s) == SelectSeq(lanes[s],LAMBDA j : j.kind = "choice")
Ownership == \A s \in S : Len(accepted[s]) = Len(completed[s]) + Len(QueuedChoices(s))
                              + IF active[s].kind = "choice" THEN 1 ELSE 0
TimeoutDrains == \A s \in finalized : completed[s] = accepted[s]
ResetDrains == drained
NoLateOutput == ~late
AdmissionClosed == pending > 0 => opened = {}
AllFinished == <> (/\ issued = Resets /\ cleaned = Resets /\ pending = 0
                   /\ \A s \in S : openpc[s] = "done" /\ requestpc[s] = Items
                               /\ timeoutpc[s] = "done" /\ active[s] = None /\ lanes[s] = <<>>)
CleanupWaiting == pending > 0 /\ \E s \in S : active[s].kind = "choice"
TimeoutBehindChoice == \E s \in S : active[s].kind = "choice" /\ \E i \in 1..Len(lanes[s]) : lanes[s][i].kind = "final"
MultipleResets == pending = 2
StaleCallback == cleaned > 0 /\ \E s \in S : openpc[s] = "new"
WorkerSaturated == Cardinality(Busy) = Workers /\ \E s \in S : active[s] = None /\ Len(lanes[s]) > 0
EmptyTimeout == \E s \in finalized : accepted[s] = <<>>
============================================================
