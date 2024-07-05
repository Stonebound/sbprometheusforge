package net.stonebound.sbprometheus.config;


import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.electronwill.nightconfig.core.utils.StringUtils.split;


//ripped from phit's cwl
public class CommentedConfigSpec extends ConfigSpec {
	private final Map<List<String>, String> comments = new HashMap<>();

	public void comment(List<String> path, String comment) {
		comments.put(path, comment);
	}

	public void comment(String path, String comment) {
		comment(split(path, '.'), comment);
	}

	@Override
	public int correct(Config config) {
		return correct(config, (action, path, incorrectValue, correctedValue) -> {
		});
	}

	@Override
	public int correct(Config config, CorrectionListener listener) {
		int corrections = super.correct(config, listener);
		if (config instanceof CommentedConfig) {
			insertComments((CommentedConfig) config);
		}
		return corrections;
	}

	private void insertComments(CommentedConfig config) {
		for (Map.Entry<List<String>, String> entry : comments.entrySet()) {
			config.setComment(entry.getKey(), entry.getValue());
		}
	}
}