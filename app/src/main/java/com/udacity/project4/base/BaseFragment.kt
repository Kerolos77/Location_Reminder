package com.udacity.project4.base

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            toast(it)
        })
        _viewModel.showToast.observe(this, Observer {
            toast(it)
        })
        _viewModel.showSnackBar.observe(this, Observer {
            snackBar(it)
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            snackBar(getString(it))
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })

    }

    fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    fun snackBar(message: String,length:Int=Snackbar.LENGTH_LONG, retryAction: ((View) -> Unit)? = null) {
        Snackbar.make(requireView(), message, length)
            .setAction(R.string.retry, retryAction)
            .show()
    }
}