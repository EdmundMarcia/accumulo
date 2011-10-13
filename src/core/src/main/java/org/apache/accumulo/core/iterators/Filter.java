package org.apache.accumulo.core.iterators;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;


public abstract class Filter extends WrappingIterator implements OptionDescriber {
	@Override
    public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
	   Filter newInstance;
	   try {
	       newInstance = this.getClass().newInstance();
	   } catch (Exception e) {
	       throw new RuntimeException(e);
	   }
	   newInstance.setSource(getSource().deepCopy(env));
	   newInstance.negate = negate;
	   return newInstance;
    }

    private static final String NEGATE = "negate";
	boolean negate;

	public Filter() {}

	public Filter(SortedKeyValueIterator<Key, Value> iterator) {
		setSource(iterator);
		negate = false;
	}

	@Override
	public void next() throws IOException {
		super.next();
		findTop();
	}

	@Override
	public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
		super.seek(range, columnFamilies, inclusive);
		findTop();
	}

	protected void findTop() {
		while (getSource().hasTop() && (negate==accept(getSource().getTopKey(), getSource().getTopValue()))) {
			try {
				getSource().next();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public abstract boolean accept(Key k, Value v);

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		super.init(source, options, env);
		negate = false;
		if (options.get(NEGATE)!=null) {
			negate = Boolean.parseBoolean(options.get(NEGATE));
		}
	}

	@Override
	public IteratorOptions describeOptions() {
		return new IteratorOptions("filter","Filter accepts or rejects each Key/Value pair",
				Collections.singletonMap("negate","default false keeps k/v that pass accept method, true rejects k/v that pass accept method"),null);
	}

	@Override
	public boolean validateOptions(Map<String, String> options) {
		if (options.get(NEGATE)!=null) {
			Boolean.parseBoolean(options.get(NEGATE));
		}
		return true;
	}
	
}