package com.logisticsalliance.general;

public interface CommonConstants {

	String RX_LW = "'40','45','50'",
		DCB = "DCB", DCV = "DCV", DCX = "DCX", DCF = "DCF",
		EVT = "EVT", EVT2 = "EVT2", RX = "RX", FS ="FS",
		DC10 = "10", DC20 = "20", DC20F = "20F", DC30 = "30", DC50 = "50", DC70 = "70", 
		CCS = "CCS", SONAR = "SONAR", DT = "DT", M_O = "M-O", TBD = "TBD", COURIER = "Courier",
		LTL = "LTL", DDCT = "DDCT",
		OPEN = "OPEN", ACTIVE = "ACTIVE",
		PLAN = "Plan",
		EXCE = "EXCE", EXCEPTION = "Exception",
		TRAN = "TRAN", TRANSIT = "Transit",
		N_A = "n/a";
	int route8000 = 8000, route9600 = 9600,
		DCBi = 0, DCVi = 1, DCXi = 2, DCFi = 3,
		EVTi = 4, EVT2i = 5, RXi = 6,
		EXCE_N = 1, TRAN_N = 2;
}
