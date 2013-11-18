/*
 * Copyright (C) 2013 たんらる
 */

package fourthline.mmlTools.core;



import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import fourthline.mmlTools.UndefinedTickException;


/**
 * MML解析
 * @author たんらる
 */
public class MelodyParser {
	private String mml_src;
	private String mml_L;
	private int mml_length = -1; // for tick
	private int mml_oct = 4;

	private int tempo;
	private Map<Integer, Integer> tempoList = new TreeMap<Integer, Integer>(); // <tick, tempo>

	// for check tempo backward
	private char playingNote = ' ';
	private List<Integer> warnIndex = new ArrayList<Integer>();

	private int minNote = -1;
	private int maxNote = -1;

	protected int noteNumber = -1;
	private String gt;

	public MelodyParser(String mml) {
		this(mml, "4", 120);
	}

	public MelodyParser(String mml, String mml_L, int tempo) {
		if (mml == null)
			mml_src = "";
		else
			mml_src = mml;

		this.mml_L = mml_L;
		this.tempo = tempo;

		tempoList.put(0, tempo); // initial tempo
	}

	public int getTempo() throws UndefinedTickException {
		if (mml_length < 0)
			cals_length();

		return tempo;
	}

	public String getMmlL() throws UndefinedTickException {
		if (mml_length < 0)
			cals_length();

		return mml_L;
	}

	public List<Integer> getWarnIndex() {
		if (mml_length < 0)
			throw new NullPointerException(" non cals. ");

		return warnIndex;
	}


	public int getLength() throws UndefinedTickException {
		if (mml_length < 0)
			cals_length();

		int beat = MMLTicks.getTick("1");
		int beatLength = mml_length / beat;
		int lastbeatLength = mml_length % beat;
		System.out.println("beat: "+beatLength+" + ["+lastbeatLength+"]");

		return mml_length;
	}

	public int getMinNote() {
		return minNote;
	}

	public int getMaxNote() {
		return maxNote;
	}

	public int getNoteNumber() {
		return noteNumber;
	}

	public String getGt() {
		return gt;
	}

	public boolean checkPitch(int min, int max) {
		if ( minNote < 0 && maxNote < 0 ) {
			return true;
		}
		if ( (minNote >= min ) && (maxNote <= max) ) {
			return true;
		} else {
			return false;
		}

	}

	public void mergeParser(MelodyParser srcParser) throws UndefinedTickException {
		int len1 = this.getLength();
		int len2 = srcParser.getLength();

		if (this.tempoList.get(0) == srcParser.tempoList.get(0)) // non tempo mml
			srcParser.tempoList.remove(0);

		if (len1 <= len2) { // length merge
			this.tempoList.remove(len1);
			this.mml_length = len2;
		} else {
			srcParser.tempoList.remove(len2);
		}

		tempoList.putAll(srcParser.tempoList);
	}

	public Map<Integer, Integer> getTempoList() throws UndefinedTickException {
		if (tempoList.size() < 2)
			cals_length();

		return tempoList;
	}

	public double getPlayLengthByTempoList() throws UndefinedTickException {
		double length_total = 0.0;

		if (tempoList.size() < 2)
			cals_length();

		Iterator<Map.Entry<Integer, Integer>> iter = tempoList.entrySet().iterator();
		Map.Entry<Integer, Integer> pre_elem = iter.next();

		while ( iter.hasNext() ) {
			Map.Entry<Integer, Integer> now_elem = iter.next();
			if ( now_elem.getValue().equals(pre_elem.getValue()) ) 
				continue; // now tempo == pre tempo

			length_total += (now_elem.getKey() - pre_elem.getKey())*60 / pre_elem.getValue();

			pre_elem = now_elem;
		}

		length_total /= (96.0);

		return length_total;
	}


	private int mmlGT(String gt) throws UndefinedTickException {
		int tick = MMLTicks.getTick(gt);
		return tick;
	}

	/**
	 * ノートのmin, maxを記録する
	 * @param note
	 */
	private void noteMinMax(int note) {
		if (note <= 0) {
			return;
		}

		if ( (minNote < 0) || (maxNote < 0) ) {
			minNote = maxNote = note;
			return;
		}

		if (note < minNote) {
			minNote = note;
		}

		if (note > maxNote) {
			maxNote = note;
		}
	}


	private int noteIndex(char n1, char n2) {
		int result = -1;
		if (n1 < 'a') {
			n1 += 'a' - 'A';
		}

		switch (n1) {
		case 'c': result = 0; break;
		case 'd': result = 2; break;
		case 'e': result = 4; break;
		case 'f': result = 5; break;
		case 'g': result = 7; break;
		case 'a': result = 9; break;
		case 'b': result = 11; break;
		default : result = -1; break;
		}

		switch (n2) {
		case '+':
		case '#': result++; break;
		case '-': result--; break;
		default : break;
		}

		return result;
	}

	private void mmlOperation(String note) throws ParserWarn3ML {
		switch(note.charAt(0)) {
		case 'l': 
		case 'L':
			mml_L = note.substring(1);
			break;
		case 't':
		case 'T':
			int temp = Integer.parseInt( note.substring(1) );
			tempoList.put(mml_length, temp);
			if (temp > 0) {
				this.tempo = temp;
			}
			if ( (this.playingNote == 'r') || (this.playingNote == 'R') ) {
				throw new ParserWarn3ML();
			}
			break;
		case 'o':
		case 'O':
			mml_oct = Integer.parseInt( note.substring(1) );
			break;
		case '<':
			mml_oct--;
			break;
		case '>':
			mml_oct++;
			break;
		default:
			break;
		}
	}

	public int noteGT(String note) throws UndefinedTickException, ParserWarn3ML {
		if (!MMLTokenizer.isNote(note.charAt(0))) {
			mmlOperation(note);
			return 0;
		}

		this.playingNote = note.charAt(0);

		if ( (this.playingNote == 'n') || (this.playingNote == 'N') ) {
			noteNumber = Integer.parseInt(note.substring(1));
			noteMinMax( noteNumber );
			gt = "";
			return mmlGT(mml_L);
		}

		char note1 = note.charAt(0);
		char note2 = ' ';
		gt = mml_L;

		if (note.length() > 1) {
			int startIndex = 1;
			note2 = note.charAt(1);

			if ( (note2 == '+') || (note2 == '-') || (note2 == '#') )
				startIndex++;

			if (startIndex < note.length()) {
				gt = note.substring(startIndex);
				if (gt.startsWith("."))
					gt = mml_L+".";
			}
		}

		int noteIndex = noteIndex(note1, note2);
		noteNumber = mml_oct * 12 + noteIndex;
		if ( (note1 != 'r' ) && (note1 != 'R') ) {
			noteMinMax( noteNumber );
		} else {
			noteNumber = -1;
		}

		return mmlGT(gt);
	}

	protected void reset() {
		mml_length = 0;
		warnIndex.removeAll(warnIndex);
	}


	/**
	 * cals mml length
	 */
	private int cals_length() throws UndefinedTickException {
		MMLTokenizer mt = new MMLTokenizer(mml_src);
		int i = 1;
		reset();

		while (mt.hasNext()) {
			int parseIndex = mt.getIndex();
			String item = mt.next();
			System.out.println(i + ": " + item + " ");

			try {
				mml_length += noteGT(item);
			} catch (ParserWarn3ML warn) {
				System.err.println(warn.getMessage()+parseIndex);
				warnIndex.add(parseIndex);
			}
			i++;
		}

		System.out.println();

		try {
			mmlOperation("T0");
		} catch (ParserWarn3ML warn) {}

		return mml_length;
	}
}