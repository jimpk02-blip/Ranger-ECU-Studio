# Ford investigation module — v1.2.12

Read-only allowlist; no reset, session change, arbitrary service scan or writes. Physical addressing follows ELM327 CAN documentation (7E0/7E8): https://www.mouser.com/datasheet/2/813/ELM327DS-2489024.pdf

Ford enhanced candidate 22 1E1C (transmission temperature): https://support.obdlink.com/support/solutions/articles/43000708843-display-transmission-temperature-gauge . Not verified on 2007 Ranger US 3.0. Report raw bytes, no inferred units. Negative or absent response is not a capability inventory. No repeat or security access.

The saved trace is a solicited ELM request/response log, not a bus-wide sniffer. Restore functional addressing and headers off or disconnect. Preserve existing permanent signing. KAM command remains unverified.
