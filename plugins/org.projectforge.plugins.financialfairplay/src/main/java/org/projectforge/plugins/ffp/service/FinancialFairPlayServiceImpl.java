package org.projectforge.plugins.ffp.service;

import org.projectforge.plugins.ffp.repository.FFPEventDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialFairPlayServiceImpl implements FinancialFairPlayService
{

  @Autowired
  private FFPEventDao eventDao;

}
